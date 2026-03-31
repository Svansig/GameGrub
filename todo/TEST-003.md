# TEST-003 - Add download/install state machine tests

- **ID**: `TEST-003`
- **Area**: `service`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Download/install state transitions can regress silently without focused tests.

## Scope

- In scope:
  - Model expected state transitions for start/pause/resume/cancel/complete.
  - Add tests for transitions and edge cases.
- Out of scope:
  - Full network integration simulation.

## Acceptance Criteria

- [ ] State transition table documented in tests.
- [ ] Tests cover normal and failure transitions.
- [ ] Regressions can be localized quickly by test failures.

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*Download*"`

## Links

- Related docs: `todo/REL-002.md`
- PR: `TBD`

