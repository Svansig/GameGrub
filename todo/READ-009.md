# READ-009 - Normalize naming in launch/container abstractions

- **ID**: `READ-009`
- **Area**: `container + launch + ui`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected naming convention updates.`
- **Reviewer**: `TBD`

## Problem

Launch/container abstractions use mixed naming styles that obscure intent.

## Scope

- In scope:
  - Define naming map and rename selected symbols.
- Out of scope:
  - Broad behavioral refactor.

## Dependencies and Decomposition

- Parent ticket: `todo/READ-002.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/COH-011.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Naming map is documented.
- [ ] Selected abstractions renamed consistently.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] `./gradlew lintKotlin`
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

