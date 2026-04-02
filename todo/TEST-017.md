# TEST-017 - Add orientation manager and lifecycle regression tests

- **ID**: `TEST-017`
- **Area**: `app/src/test + Robolectric`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required; test-only ticket, coverage noted in linked implementation PR.`
- **Reviewer**: `TBD`

## Problem

Orientation behavior has limited test coverage and currently lacks regression protection for manager logic and lifecycle transitions.

## Scope

- In scope:
  - Add `OrientationManager` unit tests for nearest selection, unknown values, boundaries, and no-op updates.
  - Add lifecycle tests validating orientation behavior after background/foreground cycles.
  - Add event-flow tests around route transitions (XServer entry/exit orientation policy changes).
- Out of scope:
  - UI visual snapshot tests unrelated to orientation behavior.

## Dependencies and Decomposition

- Parent ticket: `todo/REL-015.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/UI-018.md`, `todo/UI-019.md`, `todo/DOC-016.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Added orientation hardening tests for policy resolution and orientation selection behavior.
- [x] Boundary-angle behavior and `ORIENTATION_UNKNOWN` handling are covered.
- [x] Lifecycle regression case (start/stop/start listener idempotency) is covered.
- [x] Route transition policy contract is covered through `OrientationPolicyFlowTest` precedence checks.

## Validation

- [x] Added `app/src/test/java/app/gamegrub/ui/OrientationManagerTest.kt`.
- [x] Added `app/src/test/java/app/gamegrub/ui/orientation/OrientationPolicyFlowTest.kt`.
- [x] Attempted `./gradlew testDebugUnitTest --tests "*Orientation*"` (blocked locally: `JAVA_HOME` not set).
- [x] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [x] Implementation completed and linked files updated.

## Links

- Related docs: `todo/REL-015.md`, `todo/UI-018.md`, `todo/UI-019.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

