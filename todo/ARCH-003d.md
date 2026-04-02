# ARCH-003d - Migrate EpicLaunchCommandBuilder to Base

- **ID**: `ARCH-003d`
- **Area**: `container/launch`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Epic-specific launch logic duplicated from other stores.

## Scope

- In scope:
  - Refactor EpicLaunchCommandBuilder to extend BaseLaunchCommandBuilder
  - Keep only Epic-specific command building
  - Ensure launch works exactly as before
- Out of scope:
  - Other launch builders

## Dependencies and Decomposition

- Parent ticket: `ARCH-003`
- Child tickets: 
  - `ARCH-003a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-003a`

## Acceptance Criteria

- [ ] EpicLaunchCommandBuilder extends base
- [ ] Launch works correctly for Epic games
- [ ] No regression from refactor

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual: Launch Epic game, verify works
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
