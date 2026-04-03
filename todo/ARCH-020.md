# ARCH-020 - Implement Gateway DI Bindings

- **ID**: `ARCH-020`
- **Area**: `di`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Need to wire up gateways for UI consumption.

## Scope

- In scope:
  - Implement LibraryGateway with GameRepository
  - Implement AuthGateway with auth managers
  - Implement LaunchGateway with launch flow
  - Implement DownloadGateway with download managers
  - Bind in Hilt module
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-017`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-017`

## Acceptance Criteria

- [ ] All gateways injectable
- [ ] UI can use gateways

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
