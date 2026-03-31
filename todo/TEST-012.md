# TEST-012 - Add regression tests for settings persistence flows

- **ID**: `TEST-012`
- **Area**: `ui/screen/settings + PrefManager`
- **Priority**: `P3`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected settings test coverage docs update.`
- **Reviewer**: `TBD`

## Problem

Settings persistence regressions are easy to miss without focused tests.

## Scope

- In scope:
  - Add tests for core settings persistence and restoration.
  - Cover restart-sensitive settings where practical.
- Out of scope:
  - Full settings UI test suite.

## Dependencies and Decomposition

- Parent ticket: `todo/TEST-006.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/REL-010.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Persistence tests added for core settings.
- [ ] Restart-like restoration behavior validated.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*Settings*"`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `todo/TEST-001.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

