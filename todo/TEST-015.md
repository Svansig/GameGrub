# TEST-015 - Add contract tests for launch request gateway and pending-launch state behavior

- **ID**: `TEST-015`
- **Area**: `app/src/test`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected test strategy updates for gateway migration.`
- **Reviewer**: `TBD`

## Problem

Gateway migration for launch requests needs regression protection for queue semantics.

## Scope

- In scope:
  - Add contract tests for gateway-based launch request operations.
  - Cover pending-launch consume/peek edge cases.
- Out of scope:
  - End-to-end launch flow testing.

## Dependencies and Decomposition

- Parent ticket: `todo/COH-015.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/UI-016.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Contract tests enforce expected pending-launch behavior.
- [ ] Migration regressions are caught by unit tests.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*LaunchRequest*"`
- [ ] `./gradlew lintKotlin`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `todo/TEST-001.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

