# TEST-021 - Add regression tests for extracted timing and retry policy constants

- **ID**: `TEST-021`
- **Area**: `app/src/test + ui/model + ui/launch`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Adds regression coverage for refactor safety.
- **Reviewer**: `TBD`

## Problem

Timing/retry literal extraction can accidentally alter behavior without obvious compile-time failures; focused tests are needed to lock down policy behavior.

## Scope

- In scope:
  - Add/extend unit tests for connection timeout counting and launch retry cadence/caps.
  - Validate behavior through constants-backed execution paths.
  - Keep test scope deterministic and fast.
- Out of scope:
  - New runtime features or broader performance benchmarks.

## Dependencies and Decomposition

- Parent ticket: `todo/REL-017.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/TEST-011.md`, `todo/TEST-010.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] At least one test asserts connection timeout behavior against configured policy constants.
- [ ] At least one test asserts launch retry cap/interval behavior against configured policy constants.
- [ ] Tests fail if constants drift in a behavior-breaking way.

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*MainViewModel*"`
- [ ] `./gradlew testDebugUnitTest --tests "*Launch*"`
- [ ] `./gradlew lintKotlin`

## Links

- Related docs: `docs/magic-literals-audit-2026-04-04.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

