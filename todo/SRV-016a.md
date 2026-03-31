# SRV-016a - Add download orchestration methods to SteamInstallDomain

- **ID**: `SRV-016a`
- **Area**: `service/steam`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `Sisyphus`

## Problem

SteamInstallDomain already has `downloadJobs` map but download entry points are still in companion. First step is adding the orchestration methods to domain.

## Scope

- In scope:
  - Add `downloadApp(appId: Int)` entry point to SteamInstallDomain
  - Add `downloadApp(appId, dlcAppIds, isUpdateOrVerify)` overload
  - Add main `downloadApp(...)` with all DepotDownloader logic
  - Add `completeAppDownload(...)` method
  - Add `AppDownloadListener` as inner class or separate file
- Out of scope:
  - Updating call sites

## Acceptance Criteria

- [x] All download orchestration methods exist in SteamInstallDomain
- [x] Methods properly delegate to installDomain, libraryDomain, etc.

## Validation

- [x] Build passes (no LSP errors)

## Links

- Parent: `SRV-001`
- Related: `docs/steam-service-ownership-matrix.md`

## Notes

- Moved `downloadApp` overloads, `completeAppDownload`, and `AppDownloadListener` to domain
- Temporary coupling to `SteamService.instance` for steamClient access - parent ticket should address this
- Now ready for SRV-016b (migrating call sites)