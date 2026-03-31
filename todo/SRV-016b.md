# SRV-016b - Migrate download call sites to SteamInstallDomain

- **ID**: `SRV-016b`
- **Area**: `service/steam`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`

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

- [ ] All call sites migrated to domain
- [ ] Wrapper methods removed from companion

## Validation

- [ ] Build passes
- [ ] Manual test: download works

## Links

- Parent: `SRV-001`
- Related: `SRV-016a`