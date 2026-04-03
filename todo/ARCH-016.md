# ARCH-016 - Create Gateway Implementations

- **ID**: `ARCH-016`
- **Area**: `gateway`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Gateway interfaces created but need implementations.

## Scope

- In scope:
  - Implement LibraryGateway using GameRepository
  - Implement AuthGateway delegating to auth managers
  - Implement LaunchGateway using launch flow
  - Implement DownloadGateway using download managers
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-011`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] All gateways implemented with Hilt injection
- [ ] UI can use gateways instead of static calls

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
