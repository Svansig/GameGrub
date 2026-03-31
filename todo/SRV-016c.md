# SRV-016c - Clean up download-related imports and constants

- **ID**: `SRV-016c`
- **Area**: `service/steam`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `Sisyphus`

## Problem

After download orchestration is fully in domain, clean up SteamService imports and constants related to downloads.

## Scope

- In scope:
  - Remove DepotDownloader imports from SteamService
  - Remove download-related constants (MAX_PICS_BUFFER, INVALID_APP_ID, etc.) if no longer needed
  - Verify no remaining download-related state in service
- Out of scope:
  - Changes to download logic

## Acceptance Criteria

- [x] DepotDownloader imports removed from SteamService
- [x] Build passes

## Validation

- [x] Build passes (no LSP errors)

## Links

- Parent: `SRV-001`
- Related: `SRV-016a`, `SRV-016b`

## Notes

- Done as part of SRV-016b commit - removed imports when removing download code