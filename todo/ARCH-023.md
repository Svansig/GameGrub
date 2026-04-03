# ARCH-023 - Migrate App Screens to Use GameRepository

- **ID**: `ARCH-023`
- **Area**: `ui/screen/library`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

App screens still use store-specific services directly.

## Scope

- In scope:
  - Migrate SteamAppScreen to use GameRepository
  - Migrate GOGAppScreen to use GameRepository
  - Migrate EpicAppScreen to use GameRepository
  - Migrate AmazonAppScreen to use GameRepository
- Out of scope:
  - Auth screens

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-022`

## Acceptance Criteria

- [ ] All screens use GameRepository
- [ ] Work for all stores

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
