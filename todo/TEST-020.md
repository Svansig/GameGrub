# TEST-020 - Add regression tests for cross-platform install/move flows under storage boundary

- **ID**: `TEST-020`
- **Area**: `app/src/test + service + ui`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required; test-only coverage expansion.`
- **Reviewer**: `TBD`

## Problem

Cross-platform install/move behavior can regress during storage ownership migration without flow-level tests.

## Scope

- In scope:
  - Add regression tests for Steam/GOG/Epic/Amazon install/move path behavior.
  - Add migration and permission transition tests for representative UI/service flows.
  - Validate marker and path persistence interactions after moves.
- Out of scope:
  - Full instrumented test matrix.

## Dependencies and Decomposition

- Parent ticket: `todo/SRV-037.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/UI-021.md`, `todo/UI-023.md`, `todo/SRV-036.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Regression tests cover cross-platform install/move scenarios.
- [ ] Permission/path edge cases are exercised for migrated flows.
- [ ] Behavior parity with pre-migration baseline is verified.

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*Install*Move*Storage*"`
- [ ] `./gradlew testDebugUnitTest --tests "*Steam*|*GOG*|*Epic*|*Amazon*"`

## Links

- Related docs: `todo/SRV-037.md`, `todo/SRV-036.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

