# ARCH-018 - Migrate Services to Use GameStoreService Base

- **ID**: `ARCH-018`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Services still use duplicate boilerplate code.

## Scope

- In scope:
  - Refactor GOGService to extend GameStoreService
  - Refactor EpicService to extend GameStoreService
  - Refactor AmazonService to extend GameStoreService
  - Move store-specific logic to managers
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-002a`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] All 3 services extend GameStoreService
- [ ] Service code reduced by 30%
- [ ] All functionality preserved

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
