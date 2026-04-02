# ARCH-005a - Define GameStoreDownloader Interface

- **ID**: `ARCH-005a`
- **Area**: `service/download`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate download logic causing duplication.

## Scope

- In scope:
  - Analyze all existing download managers
  - Design unified interface:
    - download(gameId): Start download
    - pause(gameId): Pause download
    - resume(gameId): Resume download  
    - cancel(gameId): Cancel download
    - getProgress(gameId): Get progress 0.0-1.0
    - getStatus(gameId): Get DownloadInfo
  - Document protocol differences
- Out of scope:
  - Individual migrations (child tickets)

## Dependencies and Decomposition

- Parent ticket: `ARCH-005`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Analysis of all download managers
- [ ] `GameStoreDownloader` interface defined
- [ ] Design documentation

## Validation

- [ ] Design reviewed
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
