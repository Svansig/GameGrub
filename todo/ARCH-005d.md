# ARCH-005d - Migrate Amazon Download to Interface

- **ID**: `ARCH-005d`
- **Area**: `service/download`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Amazon download should implement unified interface.

## Scope

- In scope:
  - Refactor AmazonDownloadManager to implement GameStoreDownloader
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

- [ ] AmazonDownloadManager implements GameStoreDownloader
- [ ] Download works for Amazon games

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Download Amazon game
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
