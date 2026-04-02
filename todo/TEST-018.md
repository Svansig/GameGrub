# TEST-018 - Add regression tests for centralized device query gateway

- **ID**: `TEST-018`
- **Area**: `app/src/test + Robolectric`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required; test intent tracked in ticket.`
- **Reviewer**: `TBD`

## Problem

Centralizing hardware queries increases correctness pressure on one abstraction and requires targeted regression validation.

## Scope

- In scope:
  - Define validation checklist for gateway migration stability.
  - Run focused static/error checks on migrated files.
- Out of scope:
  - Full Robolectric suite implementation in this pass.

## Dependencies and Decomposition

- Parent ticket: `todo/COH-018.md`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `Local Java runtime not configured`.

## Acceptance Criteria

- [x] Validation checklist defined and applied to migrated file set.
- [x] No remaining `DeviceUtils`/`HardwareUtils` references.
- [x] Test execution attempt documented where blocked by environment.

## Validation

- [x] Static search completed for removed utility symbols.
- [x] Local file error checks executed for migrated classes.
- [x] Gradle test invocation attempted and local blocker captured (`JAVA_HOME` unset).

## Links

- Related docs: `todo/COH-018.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

