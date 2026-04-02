# ARCH-007b - Wrap Steam Cloud Saves in Interface

- **ID**: `ARCH-007b`
- **Area**: `service/cloud`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Steam cloud saves should implement unified interface.

## Scope

- In scope:
  - Create SteamCloudSavesWrapper implementing GameStoreCloudSaves
  - Wrap existing SteamAutoCloud
  - Ensure same sync flow works
- Out of scope:
  - Other stores

## Dependencies and Decomposition

- Parent ticket: `ARCH-007`
- Child tickets: 
  - `ARCH-007a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-007a`

## Acceptance Criteria

- [ ] SteamCloudSavesWrapper implements GameStoreCloudSaves
- [ ] Cloud sync works for Steam games

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Trigger Steam cloud sync
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
