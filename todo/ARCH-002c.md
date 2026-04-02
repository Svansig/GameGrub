# ARCH-002c - Migrate EpicService to Base Class

- **ID**: `ARCH-002c`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

EpicService has duplicated boilerplate that should be in base class.

## Scope

- In scope:
  - Refactor EpicService to extend GameStoreService
  - Move store-specific logic to EpicManager
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

- [ ] EpicService extends GameStoreService
- [ ] All Epic-specific logic in EpicManager
- [ ] Same functionality as before refactor
- [ ] Epic library sync works

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual: Launch EpicService, sync library, verify games appear
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
