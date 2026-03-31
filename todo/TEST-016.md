# TEST-016 - Add regression tests for gateway-backed preferences and service facades

- **ID**: `TEST-016`
- **Area**: `app/src/test + service tests`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected test coverage notes for global-state migration.`
- **Reviewer**: `TBD`

## Problem

Replacing global preference/service facades requires explicit regression tests for parity.

## Scope

- In scope:
  - Add tests for gateway-backed preference reads/writes.
  - Add service-facade replacement regression checks for migrated paths.
- Out of scope:
  - Full integration test expansion.

## Dependencies and Decomposition

- Parent ticket: `todo/COH-016.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/SRV-024.md`, `todo/SRV-026.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Parity tests exist for selected gateway-backed preference flows.
- [ ] Parity tests cover selected service-facade replacement paths.
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

- Related docs: `todo/TEST-001.md`, `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

