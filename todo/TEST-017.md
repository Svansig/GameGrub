# TEST-017 - Add orientation manager and lifecycle regression tests

- **ID**: `TEST-017`
- **Area**: `app/src/test + Robolectric`
- **Priority**: `P1`
- **Status**: `Backlog`
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

- [ ] Tests fail on current known regressions and pass with orientation hardening fixes.
- [ ] Boundary-angle behavior and `ORIENTATION_UNKNOWN` handling are covered.
- [ ] Lifecycle regression case (stop/resume orientation handling) is covered.
- [ ] Route transition orientation policy behavior is covered.

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*Orientation*"`
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintKotlin`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `todo/REL-015.md`, `todo/UI-018.md`, `todo/UI-019.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

