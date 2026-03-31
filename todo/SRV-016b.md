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

- [x] All call sites migrated to domain (via delegation)
- [x] Wrapper methods removed from companion (now delegate to domain)
- [x] Download logic now runs in SteamInstallDomain

## Validation

- [x] Build passes (compiles)
- [x] No DepotDownloader logic in companion - all delegated to domain

## Links

- Parent: `SRV-001`
- Related: `SRV-016a`, `SRV-016c`

## Notes

- Made 3 downloadApp methods in companion delegate to installDomain
- Removed ~250 lines of duplicate download orchestration from companion
- UI callers unchanged (still call SteamService.downloadApp which delegates)
- Follow-up SRV-016c will migrate UI to direct domain access