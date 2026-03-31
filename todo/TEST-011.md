# TEST-011 - Add deterministic fake clock utilities for time-based tests

- **ID**: `TEST-011`
- **Area**: `app/src/test`
- **Priority**: `P3`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected testing utilities docs update.`
- **Reviewer**: `TBD`

## Problem

Time-dependent tests can be flaky without deterministic clock control.

## Scope

- In scope:
  - Add fake clock utilities and usage guidance.
  - Migrate representative time-based tests.
- Out of scope:
  - Refactoring all existing tests.

## Dependencies and Decomposition

- Parent ticket: `todo/TEST-008.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/TEST-007.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Fake clock helper utilities added.
- [ ] Representative tests migrated to deterministic time control.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] `./gradlew testDebugUnitTest`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

