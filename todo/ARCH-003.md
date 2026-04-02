# ARCH-003 - Unified GameStoreLaunchStrategy

- **ID**: `ARCH-003`
- **Area**: `container/launch`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Launch command building has some abstraction (StoreLaunchCommandBuilder interface) but:
- Each builder (SteamLaunchCommandBuilder, GogLaunchCommandBuilder, etc.) is completely separate
- Common launch flow (container setup, environment, command) is duplicated
- Adding new stores requires duplicating all common launch logic

## Scope

- In scope:
  - Create abstract `BaseLaunchCommandBuilder` with common launch flow
  - Extract common container setup, environment setup
  - Keep store-specific command building in subclasses
  - Add new stores should only require store-specific parts
- Out of scope:
  - Service layer changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-003a` - Design and implement BaseLaunchCommandBuilder
  - `ARCH-003b` - Migrate SteamLaunchCommandBuilder to base
  - `ARCH-003c` - Migrate GogLaunchCommandBuilder to base
  - `ARCH-003d` - Migrate EpicLaunchCommandBuilder to base
  - `ARCH-003e` - Migrate AmazonLaunchCommandBuilder to base
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `BaseLaunchCommandBuilder` with common launch flow
- [ ] Container setup abstracted
- [ ] Environment setup abstracted
- [ ] All 5 launchers use base class
- [ ] Adding new store requires only store-specific parts

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual: Launch game from each store
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
