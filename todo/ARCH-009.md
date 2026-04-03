# ARCH-009 - Service Locator Cleanup

- **ID**: `ARCH-009`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

All services use companion object as service locator pattern (SteamService.getInstance(), GOGService.getInstance(), etc.). This is hard to test and creates global state.

## Scope

- In scope:
  - Replace companion object access with Hilt injection
  - Make services testable
  - Move to constructor injection pattern
- Out of scope:
  - UI layer changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-002`
- Child tickets: 
  - `ARCH-009a` - Replace SteamService companion with injection
  - `ARCH-009b` - Replace GOGService companion with injection
  - `ARCH-009c` - Replace EpicService companion with injection
  - `ARCH-009d` - Replace AmazonService companion with injection
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002` (needs base service)

## Acceptance Criteria

- [ ] All services use Hilt injection
- [ ] No static getInstance() calls in business logic
- [ ] Services are unit testable

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
