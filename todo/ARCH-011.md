# ARCH-011 - UI State Gateway Pattern

- **ID**: `ARCH-011`
- **Area**: `ui/model + service`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

UI has direct access to services and PrefManager via static calls. Need gateway pattern to make UI testable and decoupled.

## Scope

- In scope:
  - Create LibraryGateway for library operations
  - Create AuthGateway for auth operations
  - Create LaunchGateway for launch operations
  - Remove static service access from UI
- Out of scope:
  - ViewModel implementation

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-011a` - Create LibraryGateway interface
  - `ARCH-011b` - Create AuthGateway interface
  - `ARCH-011c` - Create LaunchGateway interface
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002` (needs service abstraction)

## Acceptance Criteria

- [ ] All 3 gateways created
- [ ] UI uses gateways instead of static calls
- [ ] UI is unit testable with mock gateways

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
