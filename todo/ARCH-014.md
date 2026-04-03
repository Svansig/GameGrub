# ARCH-014 - Unified Credentials Model

- **ID**: `ARCH-014`
- **Area**: `data`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate credentials models (SteamCredentials, GOGCredentials, EpicCredentials, AmazonCredentials). Need unified model.

## Scope

- In scope:
  - Create unified GameStoreCredentials in data package
  - Add GameSource discriminator
  - Update auth managers to use unified model
- Out of scope:
  - Auth flow changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-004`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Unified GameStoreCredentials created
- [ ] All stores use unified model

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
