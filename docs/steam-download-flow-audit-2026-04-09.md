# Steam Game Download Flow Audit - 2026-04-09

## Executive Summary
Comprehensive review of the complete Steam game download flow from initiation through completion. This document identifies potential issues, edge cases, and unexpected behaviors throughout the download lifecycle.

---

## 1. Download Initiation Flow

### 1.1 Entry Points
Three main paths to initiate a download:

1. **Fresh Download** (`downloadApp(appId: Int)`)
   - Path: `SteamAppScreenViewModel.downloadApp()` → `SteamInstallDomain.downloadApp(appId)`
   - Checks for existing download in memory
   - Falls back to database for partial downloads
   - Falls back to verify/update mode for already-installed apps

2. **Download with DLC Selection** (`downloadAppWithDlc()`)
   - Explicit DLC list provided
   - Path: `SteamAppScreenViewModel.downloadAppWithDlc()` → `SteamInstallDomain.downloadApp(appId, dlcAppIds, isUpdateOrVerify=false)`

3. **Verify/Update** (`verifyApp()`)
   - Path: `SteamAppScreenViewModel.verifyApp()` → `SteamInstallDomain.downloadApp(appId)`
   - Sets `isUpdateOrVerify=true`
   - Re-downloads already-installed depots to verify integrity

### 1.2 ⚠️ Issue: DLC Selection State Management

**Problem**: DLC selection in incomplete/interrupted downloads is persistent via `DownloadingAppInfo` entity, but there's no UI to re-select or modify DLC after pause/cancel.

```kotlin
// SteamInstallDomain.kt line 83-85
val downloadingAppInfo = runBlocking { libraryDomain.getDownloadingAppInfoOf(appId) }
if (downloadingAppInfo != null) {
    return downloadApp(appId, downloadingAppInfo.dlcAppIds, isUpdateOrVerify = false)
}
```

**Scenarios**:
- User starts download with DLC A selected
- User pauses/cancels download
- User later resumes: **automatically uses cached DLC A selection**
- If user wanted DLC B instead, they cannot change it without clearing the download state manually

**Impact**: Silent restoration of old DLC selections without user awareness.

---

## 2. WiFi-Only Download Enforcement

### 2.1 Multiple WiFi Checks

Downloads are blocked in 3 places when `PrefManager.downloadOnWifiOnly` is true but WiFi is unavailable:

1. **Before download initiation** (SteamInstallDomain.kt:128-134)
   ```kotlin
   private fun checkWifiOrNotify(): Boolean {
       if (PrefManager.downloadOnWifiOnly && !hasWifiOrEthernet()) {
           Timber.w("checkWifiOrNotify: no wifi and wifi-only download enabled")
           return false
       }
       return true
   }
   ```

2. **During download job** (SteamInstallDomain.kt:193)
   ```kotlin
   if (!checkWifiOrNotify()) return null
   ```

3. **Network connectivity callback** (SteamService.kt:1395-1423)
   ```kotlin
   private fun checkAndPauseDownloads() {
       if (PrefManager.downloadOnWifiOnly && !hasActiveWifiOrEthernet()) {
           for ((appId, info) in downloadJobs.entries.toList()) {
               Timber.d("Pausing download for $appId — WiFi/Ethernet lost")
               info.cancel()
               XServerRuntime.get().events.emit(AndroidEvent.DownloadPausedDueToConnectivity(appId))
               removeDownloadJob(appId)
           }
           notificationHelper.notify(getString(R.string.download_paused_wifi))
       }
   }
   ```

### 2.2 ⚠️ Issue: WiFi Check Inconsistency

**Problem**: WiFi check uses two different methods:

- `SteamInstallDomain`: Uses `NetworkManager.hasWifiOrEthernet.value` (StateFlow)
- `SteamService`: Uses `connectivityManager.activeNetwork` directly

```kotlin
// SteamInstallDomain.kt:136-138
private fun hasWifiOrEthernet(): Boolean {
    return NetworkManager.hasWifiOrEthernet.value
}

// SteamService.kt:1405-1410
private fun hasActiveWifiOrEthernet(): Boolean {
    val activeNet = connectivityManager.activeNetwork ?: return false
    val caps = connectivityManager.getNetworkCapabilities(activeNet) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}
```

**Risk**: StateFlow in `NetworkManager` may be stale if not updated frequently, while direct `ConnectivityManager` check is always fresh. This can cause:
- Download starts (old NetworkManager state shows WiFi)
- WiFi actually lost (but StateFlow stale)
- `onChunkCompleted` proceeds without WiFi pause
- Race condition between UI reporting WiFi available and actual loss

### 2.3 ⚠️ Issue: WiFi Loss During Active Download

**Behavior**:
1. Download started on WiFi
2. WiFi lost mid-transfer
3. Network callback fires, calls `checkAndPauseDownloads()`
4. Download **cancelled entirely** (not paused)
5. Progress persisted via `downloadInfo.persistProgressSnapshot()`
6. ✅ Can resume later (good)
7. ❌ User unaware downloads were cancelled (silent failure)

**Missing**: User-facing notification that download was cancelled due to connectivity loss (only Timber log exists).

---

## 3. Download Progress Tracking & Resumption

### 3.1 Multi-Layer Progress System

**DownloadInfo** tracks progress using two mechanisms:

1. **Byte-based progress** (primary, line 63-76)
   ```kotlin
   fun getProgress(): Float {
       if (totalExpectedBytes > 0L) {
           val bytesProgress = (bytesDownloaded.toFloat() / totalExpectedBytes.toFloat()).coerceIn(0f, 1f)
           return bytesProgress
       }
       // Fallback to depot-based progress...
   }
   ```

2. **Depot-based progress** (fallback, weighted)
   ```kotlin
   var total = 0f
   for (i in progresses.indices) {
       total += progresses[i] * weights[i]  // Each depot weighted by size
   }
   ```

### 3.2 ⚠️ Issue: Persisted Bytes Not Validated

**Problem**: When resuming download, persisted bytes are restored without verification against actual files on disk.

```kotlin
// SteamInstallDomain.kt:240-244
val persistedBytes = di.loadPersistedBytesDownloaded(appDirPath)
if (persistedBytes > 0L) {
    di.initializeBytesDownloaded(persistedBytes)
    Timber.i("Resumed download: initialized with $persistedBytes bytes")
}
```

**Scenarios**:
1. Download 50GB game, 25GB persisted to `.DownloadInfo/bytes_downloaded.txt`
2. User force-clears app data via Android settings (deletes game files but not `.DownloadInfo`)
3. Resume download: starts from 25GB marker
4. **DepotDownloader re-downloads already-downloaded chunks**
5. Manifest verification will fail or chunks will be re-downloaded (inefficient)

**Root Cause**: No validation that actual depot files match the persisted byte count.

### 3.3 Persistence File Location

```
<app_dir>/.DownloadInfo/bytes_downloaded.txt
```

This file:
- ✅ Persists across app kills (OS or user)
- ✅ Cleared on completion (line 464)
- ⚠️ Can become stale if depot files are deleted
- ⚠️ Not cleared on failure (only on completion)

---

## 4. Multi-App / Multi-DLC Download Orchestration

### 4.1 Downloading AppIds Tracking

`DownloadInfo.downloadingAppIds` is a `CopyOnWriteArrayList<Int>` that tracks all apps being downloaded:

```kotlin
// SteamInstallDomain.kt:229-245
val info = DownloadInfo(selectedDepots.size, appId, downloadingAppIds).also { di ->
    // ... setup ...
}

// Later, when each app completes (line 380-389)
if (mainAppDepots.isNotEmpty()) {
    completeAppDownload(info, appId, mainAppDepotIds, mainAppDlcIds, appDirPath)
}

calculatedDlcAppIds.forEach { dlcAppId ->
    val dlcDepots = selectedDepots.filter { it.value.dlcAppId == dlcAppId }
    val dlcDepotIds = dlcDepots.keys.sorted()
    completeAppDownload(info, dlcAppId, dlcDepotIds, emptyList(), appDirPath)
}
```

### 4.2 ⚠️ Issue: DLC App Completion Logic

**Problem**: `completeAppDownload` updates database and removes from `downloadingAppIds`, but only adds `DOWNLOAD_COMPLETE_MARKER` when **all apps are done**.

```kotlin
// SteamInstallDomain.kt:440-465
suspend fun completeAppDownload(
    downloadInfo: DownloadInfo,
    downloadingAppId: Int,
    entitledDepotIds: List<Int>,
    selectedDlcAppIds: List<Int>,
    appDirPath: String,
) {
    libraryDomain.upsertInstalledAppDownloadState(
        appId = downloadingAppId,
        entitledDepotIds = entitledDepotIds,
        selectedDlcAppIds = selectedDlcAppIds,
    )

    downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }

    if (downloadInfo.downloadingAppIds.isEmpty()) {
        // Only NOW add completion marker!
        withContext(Dispatchers.IO) {
            StorageManager.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            // ...
        }
    }
}
```

**Scenario**:
1. Download main app + DLC A + DLC B
2. DLC A completes, `completeAppDownload` called
3. Main app folder exists with DLC A installed but **no DOWNLOAD_COMPLETE_MARKER**
4. DLC B then fails or is cancelled
5. App folder is in inconsistent state: partial installation with no clear marker

**Worse Case**:
- DLC B fails, throws exception in try-catch (line 393-401)
- Exception handler sets weights to 0, progress to 1f
- **Still removes download job** (line 400)
- DLC A was successfully installed but main app appears incomplete

---

## 5. Listener & Event Handling

### 5.1 Progress Updates

`AppDownloadListener` in `SteamInstallDomain.kt` tracks cumulative uncompressed bytes per depot:

```kotlin
override fun onChunkCompleted(
    depotId: Int,
    depotPercentComplete: Float,
    compressedBytes: Long,
    uncompressedBytes: Long,
) {
    val previousBytes = depotCumulativeUncompressedBytes[depotId] ?: 0L
    val deltaBytes = uncompressedBytes - previousBytes
    depotCumulativeUncompressedBytes[depotId] = uncompressedBytes

    if (deltaBytes > 0L) {
        downloadInfo.updateBytesDownloaded(deltaBytes, System.currentTimeMillis())
    }

    depotIdToIndex[depotId]?.let { index ->
        downloadInfo.setProgress(depotPercentComplete, index)
    }

    downloadInfo.persistProgressSnapshot()
}
```

### 5.2 ⚠️ Issue: Listener Progress Not Synchronized

**Problem**:
1. `onChunkCompleted` called by DepotDownloader (unknown thread)
2. Updates `DownloadInfo` in-memory state (CopyOnWriteArrayList, thread-safe for mutations)
3. Persists to disk
4. **But**: No synchronization between multiple invocations

**Race Condition**:
- Thread A: `onChunkCompleted` for depot 1
- Thread B: `onChunkCompleted` for depot 2 (concurrent)
- Both read `depotCumulativeUncompressedBytes[depotId]`
- Both write deltas
- **No lost updates** (Atomic operations) but theoretically possible for bytes to be recorded twice in rapid succession

**Actual Risk**: Low (CopyOnWriteArrayList handles this), but progress persistence happens every chunk, which is disk I/O intensive.

---

## 6. Error Handling & Recovery

### 6.1 Download Failure Flow

```kotlin
// SteamInstallDomain.kt:393-401
catch (e: Exception) {
    Timber.e(e, "Download failed for app $appId")
    info.persistProgressSnapshot()
    selectedDepots.keys.sorted().forEachIndexed { idx, _ ->
        info.setWeight(idx, 0)
        info.setProgress(1f, idx)  // Mark as "complete" (misleading!)
    }
    removeDownloadJob(appId)
}
```

### 6.2 ⚠️ Issue: Misleading Failure State

**Problem**: On exception, sets progress to 1f (100%) for all depots:

```kotlin
info.setProgress(1f, idx)  // Progress is "complete"
removeDownloadJob(appId)    // Removed from active downloads
```

**Consequence**:
- App is marked as 100% complete in UI
- But `DOWNLOAD_COMPLETE_MARKER` was **never added** (exception was thrown before completion logic)
- App is in "false complete" state
- Resuming download won't work because `downloadJobs[appId]` is null
- Must manually retry or check app installation state

---

## 7. Special Cases & Edge Cases

### 7.1 Steam Controller Config Download

During download, tries to fetch Steam Controller configuration:

```kotlin
// SteamInstallDomain.kt:333-372
val appConfig = libraryDomain.getAppInfoOf(appId)?.config
if (appConfig != null) {
    tryDownloadWorkshopControllerConfig(...)
}
```

### 7.2 ⚠️ Issue: Controller Config Download Can Block Main Download

**Problem**: `tryDownloadWorkshopControllerConfig` is called **before** `depotDownloader.finishAdding()`:

```kotlin
// SteamInstallDomain.kt:333-373
tryDownloadWorkshopControllerConfig(...)

// ...

depotDownloader.finishAdding()
depotDownloader.startDownloading()
```

**Risk**: If workshop download takes 30+ seconds or fails:
- Main download hasn't started
- User sees no progress
- May think download is frozen

---

## 8. Database State During Download

### 8.1 Downloading App Info Persistence

When download starts, `DownloadingAppInfo` is saved:

```kotlin
// SteamInstallDomain.kt:225-227
runBlocking {
    libraryDomain.saveDownloadingAppInfo(appId, userSelectedDlcAppIds)
}
```

### 8.2 ⚠️ Issue: Stale Downloading App Info

**Problem**: `DownloadingAppInfo` persists through app kills/restarts, but isn't cleared until download completes **or app deletes it manually**.

**Scenario**:
1. Start download with `dlcAppIds=[99, 100]`
2. App is backgrounded → SteamService stops
3. App killed by OS (out of memory)
4. User opens app 1 week later
5. `SteamService.onCreate()` calls `reconcileInstalledFlagsSafely()` which **deletes stale `DownloadingAppInfo`** IF app is marked installed
6. If app is NOT installed: `DownloadingAppInfo` remains with stale DLC list for 1+ week

**Timing**: This is caught on service startup (line 1386-1390 of SteamService):
```kotlin
scope.launch {
    for (record in libraryDomain.getAllDownloadingApps()) {
        if (isAppInstalled(record.appId)) {
            libraryDomain.deleteDownloadingApp(record.appId)
        }
    }
}
```

But there's no equivalent cleanup for **incomplete downloads** that remain partial for weeks.

---

## 9. SteamClient Availability

### 9.1 ⚠️ Issue: No SteamClient Validation

```kotlin
// SteamInstallDomain.kt:247-252
val steamClient = SteamService.instance?.steamClient
if (steamClient == null) {
    Timber.e("SteamClient not available for download")
    return null
}
```

**Problem**: If `SteamService` crashes/stops after download is initiated:
- `SteamService.instance` becomes null
- Next call to `SteamService.requireInstance()` throws IllegalStateException
- But this check happens in the coroutine scope (line 254 onwards)
- **Throws exception during download execution**, caught by try-catch (line 393)
- Download marked as failed, but user doesn't see clear error

---

## 10. LibraryDomain Method Usage

### 10.1 ⚠️ Issue: Multiple runBlocking Calls in Non-Suspend Context

`SteamInstallDomain.kt` uses `runBlocking` extensively in non-suspend functions:

```kotlin
// Line 83
val downloadingAppInfo = runBlocking { libraryDomain.getDownloadingAppInfoOf(appId) }

// Line 89-100
val dlcAppIds = runBlocking {
    libraryDomain.getInstalledApp(appId)?.downloadedDepots.orEmpty().toMutableList()
}

// Line 141-150
val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) } ?: return emptyMap()
```

**Risk**: Blocking on IO dispatcher thread can cause:
- Deadlocks if multiple downloads try to query simultaneously
- Main thread ANR if called from main thread
- Database lock contention

---

## 11. Duplicate Logic

### 11.1 Download Completion Logic Exists in Two Places

**Location 1**: `SteamService.kt` (lines 796-830)
```kotlin
private suspend fun completeAppDownload(downloadInfo: DownloadInfo, ...) {
    instance?.libraryDomain?.upsertInstalledAppDownloadState(...)
    downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }
    if (downloadInfo.downloadingAppIds.isEmpty()) {
        StorageManager.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
        // ...
    }
}
```

**Location 2**: `SteamInstallDomain.kt` (lines 440-466)
```kotlin
suspend fun completeAppDownload(downloadInfo: DownloadInfo, ...) {
    libraryDomain.upsertInstalledAppDownloadState(...)
    downloadInfo.downloadingAppIds.removeIf { it == downloadingAppId }
    if (downloadInfo.downloadingAppIds.isEmpty()) {
        withContext(Dispatchers.IO) {
            StorageManager.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)
            // ...
        }
    }
}
```

**Issue**: Identical logic, likely copy-pasted. **Both should be using SteamInstallDomain version** (which is the one in current downloads flow).

---

## 12. Notification & Event Emission

### 12.1 Events During Download

```kotlin
// SteamInstallDomain.kt:413
notifyDownloadStarted(appId)  // Emits AndroidEvent.DownloadStatusChanged(appId, true)

// Line 463
XServerRuntime.get().events.emit(AndroidEvent.LibraryInstallStatusChanged(downloadInfo.gameId))
```

### 12.2 ⚠️ Issue: Completion Event Timing

`LibraryInstallStatusChanged` is emitted **after** database is updated but **before** persisted bytes are cleared:

```kotlin
libraryDomain.deleteDownloadingApp(downloadInfo.gameId)  // DB updated
XServerRuntime.get().events.emit(AndroidEvent.LibraryInstallStatusChanged(downloadInfo.gameId))
downloadInfo.clearPersistedBytesDownloaded(appDirPath)  // File cleared
```

**Race Condition**:
1. UI receives `LibraryInstallStatusChanged` event
2. UI queries `downloadInfo.getProgress()` → might return stale value from memory
3. UI sees inconsistent state briefly

---

## Summary of Issues Found

| Issue | Severity | Category | Location |
|-------|----------|----------|----------|
| DLC selection not re-selectable on resume | Medium | UX | SteamInstallDomain.kt:83-85 |
| WiFi check inconsistency (StateFlow vs direct) | High | Correctness | SteamInstallDomain + SteamService |
| WiFi loss silently cancels (no notification) | High | UX/Reliability | SteamService.kt:1413-1423 |
| Persisted bytes not validated on resume | High | Data Integrity | SteamInstallDomain.kt:240-244 |
| Partial DLC installations possible | High | Data Integrity | SteamInstallDomain.kt:440-465 |
| Progress set to 100% on failure (misleading) | Medium | UX | SteamInstallDomain.kt:397-398 |
| Controller config download can block main | Low | UX | SteamInstallDomain.kt:333-376 |
| Stale DownloadingAppInfo cleanup incomplete | Medium | Data Integrity | SteamService.kt:1386-1390 |
| SteamClient null check insufficient | Medium | Reliability | SteamInstallDomain.kt:248-252 |
| Excessive runBlocking in sync context | Medium | Performance | SteamInstallDomain.kt (multiple) |
| Duplicate completion logic | Low | Maintainability | SteamService.kt + SteamInstallDomain.kt |
| Event emission race condition | Low | UX | SteamInstallDomain.kt:462-464 |

---

## Recommendations

### Critical (Address Immediately)
1. **Validate persisted bytes** against actual depot files before resuming
2. **Standardize WiFi checks** - use only fresh ConnectivityManager queries
3. **Notify users of WiFi-triggered cancellations** - add user-facing message
4. **Fix partial DLC completion** - ensure all apps get marker before marking complete

### Important (Next Sprint)
1. **Add DLC re-selection UI** for interrupted downloads
2. **Consolidate completion logic** - use SteamInstallDomain version only
3. **Reduce runBlocking calls** - migrate to suspend functions
4. **Clear stale DownloadingAppInfo** - on app resume regardless of install status

### Nice-to-Have (Future)
1. **Block controller config download** until main depot download starts
2. **Fix progress state on failure** - don't set to 100%
3. **Add telemetry** for download lifecycle events


