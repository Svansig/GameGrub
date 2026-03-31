# TEST-008 - Build shared test fixture builders for game/library data

- **ID**: `TEST-008`
- **Area**: `app/src/test`
- **Priority**: `P3`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected testing utility docs update.`
- **Reviewer**: `TBD`

## Problem

Tests duplicate large fixture setup, increasing maintenance cost.

## Scope

- In scope:
  - Add reusable fixture builders for key data models.
  - Migrate representative tests to new fixtures.
- Out of scope:
  - Refactoring every existing test file.

## Dependencies and Decomposition

- Parent ticket: `todo/TEST-001.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/READ-006.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Shared fixture builders created for core models.
- [ ] Representative tests migrated with reduced duplication.
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

