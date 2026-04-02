# ARCH-005 - Unified Download Management

- **ID**: `ARCH-005`
- **Area**: `service/download`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate download managers:
- Steam uses DepotDownloader (JavaSteam library)
- GOGDownloadManager
- EpicDownloadManager
- AmazonDownloadManager

This creates:
- Duplicated download queue logic
- Inconsistent progress reporting
- No unified "downloads in progress" view in UI

## Scope

- In scope:
  - Create unified `GameStoreDownloader` interface
  - Abstract common download operations
  - Unified progress/cancel/pause interface
  - Optional: unified download queue for UI
- Out of scope:
  - Actual protocol changes (stores use different protocols)

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-005a` - Define GameStoreDownloader interface
  - `ARCH-005b` - Migrate GOG download to interface
  - `ARCH-005c` - Migrate Epic download to interface
  - `ARCH-005d` - Migrate Amazon download to interface
  - `ARCH-005e` - Wrap Steam download in interface
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002` (depends on service layer refactor)

## Acceptance Criteria

- [ ] `GameStoreDownloader` interface: download, pause, resume, cancel, getProgress
- [ ] All 5 stores implement interface
- [ ] Unified progress tracking works

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Download from each store
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
