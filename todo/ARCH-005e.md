# ARCH-005e - Wrap Steam Download in Interface

- **ID**: `ARCH-005e`
- **Area**: `service/download`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Steam uses DepotDownloader from JavaSteam library - needs wrapper to implement unified interface.

## Scope

- In scope:
  - Create SteamDownloadWrapper implementing GameStoreDownloader
  - Wrap JavaSteam DepotDownloader
  - Ensure same download flow works
- Out of scope:
  - Other stores

## Dependencies and Decomposition

- Parent ticket: `ARCH-005`
- Child tickets: 
  - `ARCH-005a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-005a`

## Acceptance Criteria

- [ ] SteamDownloadWrapper implements GameStoreDownloader
- [ ] Download works for Steam games

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Download Steam game
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
