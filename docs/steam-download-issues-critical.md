# Steam Download Flow - Critical Issues Summary

## Quick Reference: High-Impact Issues

### 🔴 CRITICAL: Data Integrity Issues

#### 1. Persisted Bytes Not Validated on Resume
**File**: `SteamInstallDomain.kt:240-244`
**Impact**: Severe - Can cause re-downloading of already-downloaded content

When download resumes, bytes are restored from `.DownloadInfo/bytes_downloaded.txt` without verifying that actual depot files exist on disk. If user deletes game files, the resumed download will have a false starting point, forcing DepotDownloader to re-verify/re-download chunks.

```kotlin
val persistedBytes = di.loadPersistedBytesDownloaded(appDirPath)
if (persistedBytes > 0L) {
    di.initializeBytesDownloaded(persistedBytes)  // ❌ No validation!
}
```

**Fix**: Validate persisted bytes against manifest/file checksums before using them.

---

#### 2. Partial Multi-App/DLC Installations Can Leave Inconsistent State
**Files**: `SteamInstallDomain.kt:380-389` + `440-465`
**Impact**: Severe - Incomplete/corrupt game installations

When downloading main app + multiple DLCs:
- Main app completes → marked in DB as installed
- DLC A completes → marked in DB as installed
- DLC B fails → exception caught, progress set to 100% (misleading)
- `DOWNLOAD_COMPLETE_MARKER` added only when ALL apps complete
- If DLC B fails, marker is never added despite partial installation

```kotlin
if (downloadInfo.downloadingAppIds.isEmpty()) {
    StorageManager.addMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER)  // Needs ALL to be done!
}
```

**Fix**: Each app should get its own completion marker. Consolidate only at end.

---

### 🟠 HIGH: Behavioral Issues

#### 3. WiFi Check Inconsistency - Race Condition Risk
**Files**: `SteamInstallDomain.kt:136-138` vs `SteamService.kt:1405-1410`
**Impact**: High - WiFi-only downloads can proceed when they shouldn't

- `SteamInstallDomain` checks `NetworkManager.hasWifiOrEthernet.value` (StateFlow, potentially stale)
- `SteamService` checks `connectivityManager.activeNetwork` directly (always fresh)

StateFlow can lag behind actual network state, causing:
```
1. Download started (UI shows WiFi available via stale StateFlow)
2. WiFi actually lost
3. Download proceeds, consuming cellular data
4. Later network callback detects loss and cancels download
```

**Fix**: Use only direct `ConnectivityManager` queries everywhere.

---

#### 4. WiFi Loss During Download - Silent Cancellation
**File**: `SteamService.kt:1413-1423`
**Impact**: High - Users unaware downloads were cancelled

When WiFi is lost mid-download:
```kotlin
if (PrefManager.downloadOnWifiOnly && !hasActiveWifiOrEthernet()) {
    for ((appId, info) in downloadJobs.entries.toList()) {
        Timber.d("Pausing download for $appId — WiFi/Ethernet lost")  // ❌ Timber.d (debug only!)
        info.cancel()
        XServerRuntime.get().events.emit(AndroidEvent.DownloadPausedDueToConnectivity(appId))
        removeDownloadJob(appId)
    }
    notificationHelper.notify(getString(R.string.download_paused_wifi))
}
```

The notification is sent, but only a Timber.d log shows it was cancelled. User may not see notification or understand what happened.

**Fix**:
- Add user-facing notification/UI alert (not just logging)
- Consider pausing instead of cancelling

---

#### 5. Download Failure Marked as 100% Complete (Misleading)
**File**: `SteamInstallDomain.kt:393-401`
**Impact**: Medium - Confusing UI state

When download throws exception:
```kotlin
catch (e: Exception) {
    Timber.e(e, "Download failed for app $appId")
    info.persistProgressSnapshot()
    selectedDepots.keys.sorted().forEachIndexed { idx, _ ->
        info.setProgress(1f, idx)  // ❌ Marked as 100% complete!
    }
    removeDownloadJob(appId)
}
```

Progress UI shows 100% but:
- `DOWNLOAD_COMPLETE_MARKER` was never added
- App is not actually installed
- Resume download will fail (no DownloadInfo in memory)

**Fix**: Set progress to 0 or keep at last known value on failure. Never set to 100%.

---

### 🟡 MEDIUM: UX/Workflow Issues

#### 6. DLC Selection Locked After Pause/Cancel
**File**: `SteamInstallDomain.kt:83-85`
**Impact**: Medium - Poor UX

When download is interrupted, DLC selection is persisted but user cannot change it:
```kotlin
val downloadingAppInfo = runBlocking { libraryDomain.getDownloadingAppInfoOf(appId) }
if (downloadingAppInfo != null) {
    return downloadApp(appId, downloadingAppInfo.dlcAppIds, isUpdateOrVerify = false)
}
```

User's only options:
- Accept old DLC selection and resume
- Manually clear download database
- Delete game folder entirely and start over

**Fix**:
- Add UI to show/modify DLC selection before resume
- Or auto-clear stale `DownloadingAppInfo` on app resume

---

#### 7. Stale DownloadingAppInfo Not Fully Cleaned Up
**File**: `SteamService.kt:1386-1390`
**Impact**: Medium - Stale state persists

On service startup, `DownloadingAppInfo` is only deleted if app is already marked installed:
```kotlin
for (record in libraryDomain.getAllDownloadingApps()) {
    if (isAppInstalled(record.appId)) {
        libraryDomain.deleteDownloadingApp(record.appId)  // ❌ Only if installed!
    }
}
```

If download was interrupted and never completed:
- `DownloadingAppInfo` remains in DB indefinitely
- DLC list persists with stale selections for weeks/months
- User can't change DLC without manual intervention

**Fix**: Clean up ALL stale `DownloadingAppInfo` on service startup, regardless of install status.

---

### 🔵 LOW: Architecture/Maintenance Issues

#### 8. Duplicate Download Completion Logic
**Files**: `SteamService.kt:796-830` + `SteamInstallDomain.kt:440-466`
**Impact**: Low - Maintenance burden, divergence risk

Identical `completeAppDownload` methods in two places. Current code uses `SteamInstallDomain` version but `SteamService` version is dead code.

**Fix**: Remove `SteamService` version. Use only `SteamInstallDomain`.

---

#### 9. Excessive runBlocking in Non-Suspend Context
**File**: Multiple locations in `SteamInstallDomain.kt`
**Impact**: Low-Medium - Performance, potential deadlocks

```kotlin
val downloadingAppInfo = runBlocking { libraryDomain.getDownloadingAppInfoOf(appId) }
val appInfo = runBlocking { libraryDomain.getAppInfoOf(appId) } ?: return emptyMap()
```

Blocks IO thread on database calls. Potential for:
- Lock contention with other concurrent downloads
- Main thread ANR if called from main thread (unlikely but possible)

**Fix**: Convert to suspend functions where possible.

---

## Testing Scenarios

These scenarios reveal the issues:

### Test: Resume After WiFi Loss
1. Start downloading game on WiFi
2. Pull network cable (simulate WiFi loss)
3. Observe: Download silently cancelled (only Timber log)
4. Expected: User notification that download was paused
5. **Fails**: No user-facing notification

### Test: Resume With Deleted Files
1. Start downloading 50GB game
2. 25GB downloaded, pause
3. User deletes game folder via file manager
4. Resume download
5. Expected: Re-downloads from 0 or validates existing files
6. **Fails**: Resumes from 25GB marker, DepotDownloader re-verifies/re-downloads chunks

### Test: Multi-DLC Failure
1. Download game + 3 DLCs
2. Main app completes OK
3. DLC 1 completes OK
4. DLC 2 fails (network error, for example)
5. Observe: Progress shows 100%, but `DOWNLOAD_COMPLETE_MARKER` never added
6. **Fails**: Inconsistent state, game marked complete but DLC 2 missing

---

## Files to Review/Modify

**Priority 1 (Critical fixes)**:
- `/home/svansig/projects/GameGrub/app/src/main/java/app/gamegrub/service/steam/domain/SteamInstallDomain.kt`
- `/home/svansig/projects/GameGrub/app/src/main/java/app/gamegrub/service/steam/SteamService.kt`
- `/home/svansig/projects/GameGrub/app/src/main/java/app/gamegrub/data/DownloadInfo.kt`

**Priority 2 (Important improvements)**:
- `/home/svansig/projects/GameGrub/app/src/main/java/app/gamegrub/network/NetworkManager.kt`
- `/home/svansig/projects/GameGrub/app/src/main/java/app/gamegrub/service/steam/domain/SteamLibraryDomain.kt`

**Priority 3 (Cleanup)**:
- Remove dead `completeAppDownload` from `SteamService.kt`


