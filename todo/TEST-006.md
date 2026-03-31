# TEST-006 - Add Compose UI state tests for critical screens

- **ID**: `TEST-006`
- **Area**: `ui`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected testing guidelines update for Compose state tests.`
- **Reviewer**: `TBD`

## Problem

Critical UI states are not consistently regression-tested in Compose.

## Scope

- In scope:
  - Identify high-risk screen states.
  - Add deterministic state rendering tests.
- Out of scope:
  - Full screenshot golden framework rollout.

## Dependencies and Decomposition

- Parent ticket: `todo/TEST-001.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/UI-004.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] State tests cover key loading/error/success paths.
- [ ] Tests are stable in local and CI execution.
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

