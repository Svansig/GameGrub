# ARCH-024 - Replace Service Companion Objects with Injection

- **ID**: `ARCH-024`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Services use companion objects as service locators, making testing difficult.

## Scope

- In scope:
  - Remove GOGService.getInstance()
  - Remove EpicService.getInstance()
  - Remove AmazonService.getInstance()
  - Replace with Hilt injection
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-018`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-018`

## Acceptance Criteria

- [ ] No companion object getInstance() calls
- [ ] All services injectable

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
