# TEST-002 - Add auth and library regression tests

- **ID**: `TEST-002`
- **Area**: `ui/model + service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Critical auth and library flows have limited automated regression protection.

## Scope

- In scope:
  - Add tests for login state transitions and library refresh behavior.
  - Cover error and cancellation branches.
- Out of scope:
  - End-to-end instrumentation for every platform.

## Acceptance Criteria

- [ ] Regression tests added for key auth flows.
- [ ] Regression tests added for library refresh/filter behavior.
- [ ] New tests are deterministic and stable.

## Validation

- [ ] `./gradlew testDebugUnitTest`

## Links

- Related docs: `docs/ui-placement/ui-validation-checklist.md`
- PR: `TBD`

