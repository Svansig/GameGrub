# SRV-016b - Migrate download call sites to SteamInstallDomain

- **ID**: `SRV-016b`
- **Area**: `service/steam`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `Sisyphus`

## Problem

After adding download methods to SteamInstallDomain, need to migrate all call sites from SteamService to domain.

## Scope

- In scope:
  - Find all callers of `SteamService.downloadApp(...)`
  - Update to call `SteamInstallDomain.downloadApp(...)` instead
  - Remove wrapper methods from companion
- Out of scope:
  - Changes to download logic itself

## Acceptance Criteria

- [x] All call sites migrated to domain
- [x] Wrapper methods removed from companion
- [x] Download logic now runs in SteamInstallDomain

## Validation

- [x] All UI call sites now use `getSteamInstallDomain(context)` to access domain directly
- [x] No `SteamService.downloadApp` or `SteamService.getAppDownloadInfo` calls remain in SteamAppScreen
- [x] Wrapper methods removed from SteamService companion

## Links

- Parent: `SRV-001`
- Related: `SRV-016a`

## Notes

### SteamAppScreen.kt
- Added `SteamInstallDomainEntryPoint` Hilt entry point interface
- Added `getSteamInstallDomain(context)` helper function
- Migrated call sites:
  - `onDownloadInstallClick` - downloadApp, getAppDownloadInfo
  - `onPauseResumeClick` - downloadApp, getAppDownloadInfo
  - `onDeleteDownloadClick` - getAppDownloadInfo
  - `onUpdateClick` - downloadApp
  - `saveContainerConfig` - downloadApp
  - `launchPendingInstall` - downloadApp with DLC params
  - `CANCEL_APP_DOWNLOAD` dialog - getAppDownloadInfo
  - `UPDATE_VERIFY_CONFIRM` dialog - downloadApp
  - `INSTALL_APP` dialog - downloadApp
  - `attachDownloadProgressListener` - getAppDownloadInfo

### BaseAppScreen.kt
- Added `SteamInstallDomainEntryPoint` Hilt entry point interface
- Added `getSteamInstallDomain(context)` helper function
- Migrated call in `getGameDisplayInfo`:
  - `GameSource.STEAM` branch - getAppDownloadInfo

### LibraryListCard.kt
- Added `SteamInstallDomainEntryPoint` Hilt entry point interface
- Added `getSteamInstallDomain(context)` helper function
- Added `import LocalContext.current` for context access in Composable
- Migrated call in `InstallStatusBadge`:
  - `remember` block - getAppDownloadInfo

### SteamService.kt
Removed wrapper methods from companion:
- `downloadApp(appId: Int)`
- `downloadApp(appId, dlcAppIds, isUpdateOrVerify)`
- `downloadApp(appId, downloadableDepots, dlcAppIds, branch, language, isUpdateOrVerify)`
- `getAppDownloadInfo(appId: Int)`