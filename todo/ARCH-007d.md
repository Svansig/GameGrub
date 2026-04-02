# ARCH-007d - Migrate Epic Cloud Saves to Interface

- **ID**: `ARCH-007d`
- **Area**: `service/cloud`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Epic cloud saves should implement unified interface.

## Scope

- In scope:
  - Refactor EpicCloudSavesManager to implement GameStoreCloudSaves
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

- [ ] EpicCloudSavesManager implements GameStoreCloudSaves
- [ ] Cloud sync works for Epic games

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Trigger Epic cloud sync
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
