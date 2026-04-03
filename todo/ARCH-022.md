# ARCH-022 - Migrate LibraryViewModel to Use GameRepository

- **ID**: `ARCH-022`
- **Area**: `ui/model`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

LibraryViewModel still uses store-specific DAOs directly.

## Scope

- In scope:
  - Refactor LibraryViewModel to use GameRepository
  - Use GameFilter for unified filtering
  - Unify sorting logic
  - Remove store-specific logic
- Out of scope:
  - Other ViewModels

## Dependencies and Decomposition

- Parent ticket: `ARCH-008`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001c`

## Acceptance Criteria

- [ ] LibraryViewModel uses GameRepository
- [ ] Uses GameFilter for filtering
- [ ] Works for all stores

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
