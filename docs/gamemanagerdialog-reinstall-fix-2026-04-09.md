# GameManagerDialog "Install" Button Reappearing Fix - April 9, 2026

## Issue
**Problem**: When user clicked "Choose install location" popup, selected "Internal", the download started, but then the GameManagerDialog reappeared.

**Root Cause**: Empty else-if block in the `onInstall` callback that prevented the storage location dialog from showing when external storage directories were available.

## Code Flow (Before Fix)

```kotlin
onInstall = { dlcAppIds ->
    hideGameManagerDialog(gameId)
    pendingInstallDlcIds = dlcAppIds
    if (storageLocationConfirmedForInstall) {
        showPendingInstallDialog()
    } else if (externalStorageDirs.isNotEmpty()) {
        // ❌ EMPTY BLOCK - DOES NOTHING!
    } else {
        PrefManager.useExternalStorage = false
        showPendingInstallDialog()
    }
}
```

## What Was Happening

1. User sees GameManagerDialog
2. Clicks "Install" button
3. `onInstall` callback fires
4. GameManagerDialog is hidden ✓
5. `pendingInstallDlcIds` is set ✓
6. `storageLocationConfirmedForInstall` is FALSE (first time)
7. `externalStorageDirs.isNotEmpty()` is TRUE (device has external storage)
8. ❌ Else-if block executes but DOES NOTHING
9. Function returns without showing storage location dialog
10. GameManagerDialog state still exists in map
11. Next recomposition: GameManagerDialog appears again

## Solution
Added line to show storage location dialog when external storage is available:

```kotlin
onInstall = { dlcAppIds ->
    hideGameManagerDialog(gameId)
    pendingInstallDlcIds = dlcAppIds
    if (storageLocationConfirmedForInstall) {
        showPendingInstallDialog()
    } else if (externalStorageDirs.isNotEmpty()) {
        showStorageLocationDialog = true  // ✅ NOW SHOWS STORAGE DIALOG!
    } else {
        PrefManager.useExternalStorage = false
        storageLocationConfirmedForInstall = true
        showPendingInstallDialog()
    }
}
```

## Expected Flow (After Fix)

1. User sees GameManagerDialog
2. Clicks "Install" button
3. `onInstall` callback fires
4. GameManagerDialog is hidden ✓
5. `pendingInstallDlcIds` is set ✓
6. Storage location dialog shows ✓
7. User selects "Internal" or "External"
8. Storage location dialog closes
9. Install pending dialog shows
10. Download starts

## Files Modified
- `/app/src/main/java/app/gamegrub/ui/screen/library/appscreen/SteamAppScreen.kt` (line 1466)

## Why This Works
The storage location dialog (`showStorageLocationDialog`) is already defined and handled. When set to true, it triggers a recomposition that shows the AlertDialog asking the user to choose between internal and external storage. Once they choose, `storageLocationConfirmedForInstall` is set to true and the pending install dialog is shown.


