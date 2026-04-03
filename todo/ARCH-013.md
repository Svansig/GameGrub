# ARCH-013 - Unify Download Info Model

- **ID**: `ARCH-013`
- **Area**: `data`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate DownloadInfo models (SteamDownloadInfo, GOGDownloadInfo, EpicDownloadInfo, AmazonDownloadInfo). Need unified model.

## Scope

- In scope:
  - Create unified DownloadInfo in data package
  - Update all download managers to use unified model
  - Update UI to use unified model
- Out of scope:
  - Service implementation changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-001`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Unified DownloadInfo created
- [ ] All stores use unified model

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
