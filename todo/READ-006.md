# READ-006 - Refactor long methods into testable helper units

- **ID**: `READ-006`
- **Area**: `ui + service`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected updates to method ownership notes in docs/tickets.`
- **Reviewer**: `TBD`

## Problem

Long methods make behavior changes risky and reviews slow.

## Scope

- In scope:
  - Identify high-churn long methods.
  - Extract helper units with clear names and focused tests.
- Out of scope:
  - Behavioral redesign.

## Dependencies and Decomposition

- Parent ticket: `todo/READ-001.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/TEST-008.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] At least two long methods are split into cohesive helpers.
- [ ] New helper units are covered with targeted tests.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintKotlin`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `todo/READ-001.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

