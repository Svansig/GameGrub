# TEST-019 - Add contract tests for storage gateways (paths, permissions, markers, file operations)

- **ID**: `TEST-019`
- **Area**: `app/src/test + Robolectric`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required; test-only coverage expansion.`
- **Reviewer**: `TBD`

## Problem

Storage gateway behavior needs regression protection before broad caller migration removes legacy utility paths.

## Scope

- In scope:
  - Add contract tests for path resolution and storage selection policies.
  - Add permission and marker lifecycle tests.
  - Add file operation fallback/error behavior tests.
- Out of scope:
  - End-to-end UI flow tests (covered in follow-up regression ticket).

## Dependencies and Decomposition

- Parent ticket: `todo/COH-020.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/TEST-020.md`, `todo/SEC-015.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Contract tests exist for all primary storage gateway interfaces.
- [ ] Marker/file/path behavior edge cases are covered.
- [ ] Test fixtures are deterministic and reusable.

## Validation

- [ ] `./gradlew testDebugUnitTest --tests "*StorageGateway*"`
- [ ] `./gradlew testDebugUnitTest --tests "*Storage*Test"`

## Links

- Related docs: `todo/COH-020.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

