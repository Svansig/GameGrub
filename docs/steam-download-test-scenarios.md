# Steam Download Flow - Test Cases & Verification Checklist

## Automated Test Cases to Add

### DownloadInfo Tests

#### Test: Progress Calculation with Byte Tracking
```kotlin
@Test
fun getProgress_usesBytesWhenAvailable() {
    val info = DownloadInfo(jobCount = 1, gameId = 123, downloadingAppIds = mutableListOf(123))
    info.setTotalExpectedBytes(1000L)
    info.updateBytesDownloaded(500L, System.currentTimeMillis())

    assertEquals(0.5f, info.getProgress(), 0.01f)
}
```

#### Test: Progress Persists Across Instances
```kotlin
@Test
fun persistenceAndRecovery() {
    val appDirPath = tempDir.path
    val info1 = DownloadInfo(1, 123, mutableListOf(123))
    info1.setTotalExpectedBytes(1000L)
    info1.updateBytesDownloaded(600L, System.currentTimeMillis())
    info1.persistProgressSnapshot()

    val info2 = DownloadInfo(1, 123, mutableListOf(123))
    val persisted = info2.loadPersistedBytesDownloaded(appDirPath)

    assertEquals(600L, persisted)
}
```

#### Test: Zero Bytes Doesn't Corrupt Persisted Value
```kotlin
@Test
fun updateBytesDownloaded_withZeroDoesntUpdate() {
    val appDirPath = tempDir.path
    val info = DownloadInfo(1, 123, mutableListOf(123))
    info.updateBytesDownloaded(100L, System.currentTimeMillis())
    info.persistProgressSnapshot()

    info.updateBytesDownloaded(0L, System.currentTimeMillis())
    info.persistProgressSnapshot()

    val recovered = DownloadInfo(1, 123, mutableListOf(123))
    assertEquals(100L, recovered.loadPersistedBytesDownloaded(appDirPath))
}
```

---

### SteamInstallDomain Tests

#### Test: Doesn't Download When WiFi Required But Unavailable
```kotlin
@Test
fun downloadApp_returnsNull_whenWifiOnlyAndNoWifi() {
    // Setup
    val prevWifiOnly = PrefManager.downloadOnWifiOnly
    val prevNetworkState = NetworkManager.hasWifiOrEthernet.value

    try {
        PrefManager.downloadOnWifiOnly = true
        // Mock NetworkManager to report no WiFi

        val result = installDomain.downloadApp(123)

        assertNull(result)
    } finally {
        PrefManager.downloadOnWifiOnly = prevWifiOnly
    }
}
```

#### Test: Resumes Download With Persisted Selection
```kotlin
@Test
fun downloadApp_resumesWithPersistedDlcSelection() = runTest {
    // Create partial download record
    val dlcIds = listOf(100, 101, 102)
    libraryDomain.saveDownloadingAppInfo(123, dlcIds)

    val result = installDomain.downloadApp(123)

    // Download job should be created with same DLC IDs
    assertNotNull(result)
    assertEquals(dlcIds, result.downloadingAppIds.toList())
}
```

#### Test: MultiDLC Completion Marks Only When All Done
```kotlin
@Test
fun completeAppDownload_addsMarkerOnlyWhenAllAppsComplete() = runTest {
    val mainAppId = 123
    val dlc1Id = 100
    val dlc2Id = 101
    val appDirPath = tempDir.path

    val info = DownloadInfo(
        jobCount = 3,
        gameId = mainAppId,
        downloadingAppIds = mutableListOf(mainAppId, dlc1Id, dlc2Id)
    )
    info.setPersistencePath(appDirPath)

    // Complete main app
    installDomain.completeAppDownload(info, mainAppId, listOf(1), emptyList(), appDirPath)
    assertFalse(StorageManager.hasMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER))

    // Complete DLC 1
    installDomain.completeAppDownload(info, dlc1Id, listOf(2), emptyList(), appDirPath)
    assertFalse(StorageManager.hasMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER))

    // Complete DLC 2
    installDomain.completeAppDownload(info, dlc2Id, listOf(3), emptyList(), appDirPath)
    assertTrue(StorageManager.hasMarker(appDirPath, Marker.DOWNLOAD_COMPLETE_MARKER))
}
```

---

### WiFi Check Tests

#### Test: WiFi Check Uses Fresh Network State
```kotlin
@Test
fun checkWifiOrNotify_usesFreshConnectivityState() {
    val prevWifiOnly = PrefManager.downloadOnWifiOnly

    try {
        PrefManager.downloadOnWifiOnly = true

        // Mock: WiFi available
        mockConnectivityManager_hasWifi()
        assertTrue(installDomain.hasWifiOrEthernet())

        // Mock: WiFi lost
        mockConnectivityManager_noWifi()
        assertFalse(installDomain.hasWifiOrEthernet())

    } finally {
        PrefManager.downloadOnWifiOnly = prevWifiOnly
    }
}
```

---

## Manual Test Scenarios

### Scenario 1: WiFi Loss During Download

**Setup**:
- Emulator with internet connection
- 500MB game download
- WiFi-only downloads enabled

**Steps**:
1. Start game download
2. Wait for 100MB to download
3. Disconnect WiFi from router (simulate loss)
4. Observe behavior for 30 seconds

**Expected**:
- Download is paused/cancelled
- User sees notification: "Download paused - WiFi lost"
- Progress is persisted

**Check For**:
- ✓ Notification appears
- ✓ Timber logs show connectivity loss
- ✓ `.DownloadInfo/bytes_downloaded.txt` contains ~100MB
- ✓ `DownloadingAppInfo` table still has record

---

### Scenario 2: Resume After Force Stop

**Setup**:
- Start download of 500MB game + 2 DLCs (500MB each)
- Let 250MB of main app download

**Steps**:
1. Start download
2. Wait for main app to reach 250MB (halfway)
3. Force stop app (Settings → Apps → Force Stop)
4. Reopen app
5. Check if resume prompt appears
6. Resume download

**Expected**:
- Persisted progress (~250MB) is used
- Download resumes from ~250MB mark
- Main app completes, DLCs start
- Final marker added when everything complete

**Check For**:
- ✓ Resume UI prompts user
- ✓ No duplicate downloads of 0-250MB range
- ✓ Eventual completion
- ✓ No "false complete" state (100% but not really done)

---

### Scenario 3: WiFi Available But Fails Mid-Download

**Setup**:
- Download 1GB game on WiFi
- Have network proxy that can fail after N bytes

**Steps**:
1. Start download
2. Let 500MB download
3. Proxy fails (simulate network timeout)
4. Observe error handling

**Expected**:
- Download stops (not silently cancelled)
- Error message shown
- Progress persisted
- User can retry

**Check For**:
- ✓ Error message appears
- ✓ Progress is not at 100% (should be ~500MB)
- ✓ DB record still exists
- ✓ Bytes file persisted

---

### Scenario 4: Multi-DLC Partial Failure

**Setup**:
- Game with 3 DLCs
- Network proxy set to fail after downloading main + 1st DLC

**Steps**:
1. Start download with all DLCs selected
2. Let main app + DLC 1 download fully
3. Wait for DLC 2 to fail
4. Observe folder state

**Expected**:
- Main app folder exists with SOME completion
- DLC 1 installed
- DLC 2 partially downloaded or missing
- No `DOWNLOAD_COMPLETE_MARKER` on folder
- App shown as "Incomplete" in UI

**Check For**:
- ✓ App folder is accessible
- ✓ No `DOWNLOAD_COMPLETE_MARKER`
- ✓ UI shows "Incomplete" or "Paused"
- ✓ Not shown as "Installed"

---

### Scenario 5: Cellular Download with WiFi-Only Setting

**Setup**:
- WiFi-only downloads enabled
- Only cellular available

**Steps**:
1. Try to start download on cellular only

**Expected**:
- Download rejected immediately
- User sees error: "WiFi required"
- No download job created

**Check For**:
- ✓ No `DownloadingAppInfo` record created
- ✓ No `DownloadInfo` in memory
- ✓ Error notification shown
- ✓ No cellular data consumed

---

### Scenario 6: App Data Deletion Between Sessions

**Setup**:
- Download 50GB game to 25GB
- App killed

**Steps**:
1. Start download (25GB)
2. Kill app (via Settings → Apps → Clear Data, but leave game folder)
3. Actually delete game folder via file manager
4. Reopen app
5. Try to resume download

**Expected**:
- Attempted resume detected
- Manifest verification would fail or re-download
- Eventually succeeds but slowly due to re-verification

**Check For**:
- ✓ No crash on resume attempt
- ✓ System detects folder deletion
- ✓ Bytes file (25GB) is orphaned and unused

---

### Scenario 7: Rapid WiFi Toggle

**Setup**:
- WiFi-only downloads enabled
- Download 500MB game
- Toggle WiFi on/off every 3 seconds

**Steps**:
1. Start download
2. WiFi on → download starts
3. WiFi off → download paused
4. WiFi on → download resumes
5. Repeat 5 times

**Expected**:
- Download pauses/resumes correctly
- No data corruption
- Final state is either complete or paused (never inconsistent)

**Check For**:
- ✓ Each pause is clean
- ✓ Each resume uses persisted bytes
- ✓ No "ghost" download jobs remaining
- ✓ UI stays in sync

---

## Verification Checklist

### Pre-Release
- [ ] Byte persistence test passes
- [ ] WiFi-only enforcement works
- [ ] Multi-DLC downloads to completion
- [ ] Incomplete marker not added prematurely
- [ ] WiFi loss triggers notification (not silent cancel)
- [ ] Resume shows resumed progress, not 0%
- [ ] Force stop doesn't leave orphaned records

### Database State
- [ ] `DownloadingAppInfo` cleared on app completion
- [ ] `DownloadingAppInfo` cleared on install detection
- [ ] `AppInfo` marked with correct `downloadedDepots`
- [ ] No duplicate `downloading_app_info` records

### File System State
- [ ] `.DownloadInfo/bytes_downloaded.txt` created only during download
- [ ] Persisted bytes file cleared on completion
- [ ] Persisted bytes file NOT cleared on failure
- [ ] `DOWNLOAD_COMPLETE_MARKER` added after ALL apps complete
- [ ] Markers not added if any DLC fails

### Event Emission
- [ ] `DownloadStatusChanged(appId, true)` on start
- [ ] `DownloadStatusChanged(appId, false)` on removal
- [ ] `LibraryInstallStatusChanged` on completion
- [ ] No duplicate events


