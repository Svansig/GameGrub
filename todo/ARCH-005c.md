# ARCH-005c - Migrate Epic Download to Interface

- **ID**: `ARCH-005c`
- **Area**: `service/download`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Epic download should implement unified interface.

## Scope

- In scope:
  - Refactor EpicDownloadManager to implement GameStoreDownloader
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

- [ ] EpicDownloadManager implements GameStoreDownloader
- [ ] Download works for Epic games

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Download Epic game
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
