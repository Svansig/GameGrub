# ARCH-007 - Cloud Saves Unification

- **ID**: `ARCH-007`
- **Area**: `service/cloud`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate cloud saves handling:
- SteamAutoCloud / SteamCloudSavesManager
- GOGCloudSavesManager
- EpicCloudSavesManager
- Amazon has no cloud saves

This creates duplicated sync logic and inconsistent behavior.

## Scope

- In scope:
  - Create unified `GameStoreCloudSaves` interface
  - Abstract common sync operations
  - Unified sync trigger and conflict resolution
  - Migrate all implementations
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-007a` - Define GameStoreCloudSaves interface
  - `ARCH-007b` - Wrap Steam cloud saves in interface
  - `ARCH-007c` - Migrate GOG cloud saves to interface
  - `ARCH-007d` - Migrate Epic cloud saves to interface
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002` (depends on service layer refactor)

## Acceptance Criteria

- [ ] `GameStoreCloudSaves` interface: sync, getStatus, resolveConflict
- [ ] All implementations use interface
- [ ] Common sync UI works

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Trigger cloud sync for each store
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
