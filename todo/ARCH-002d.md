# ARCH-002d - Migrate AmazonService to Base Class

- **ID**: `ARCH-002d`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

AmazonService has duplicated boilerplate that should be in base class.

## Scope

- In scope:
  - Refactor AmazonService to extend GameStoreService
  - Move store-specific logic to AmazonManager
  - Ensure feature parity maintained
- Out of scope:
  - Other services

## Dependencies and Decomposition

- Parent ticket: `ARCH-002`
- Child tickets: 
  - `ARCH-002a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002a`

## Acceptance Criteria

- [ ] AmazonService extends GameStoreService
- [ ] All Amazon-specific logic in AmazonManager
- [ ] Same functionality as before refactor
- [ ] Amazon library sync works

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual: Launch AmazonService, sync library, verify games appear
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
