# TEST-004 - Add smoke tests for launch and resume flows

- **ID**: `TEST-004`
- **Area**: `ui + service + xserver`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Launch and resume paths are high-risk and currently under-tested.

## Scope

- In scope:
  - Add smoke tests for launch request handling and resume paths.
  - Include failure fallback behavior checks.
- Out of scope:
  - Full device farm strategy.

## Acceptance Criteria

- [ ] Smoke tests added for key launch entry points.
- [ ] Resume/exit edge cases have basic coverage.
- [ ] Tests run reliably in local development.

## Validation

- [ ] `./gradlew testDebugUnitTest`

## Links

- Related docs: `docs/ui-placement/ui-migration-playbook.md`
- PR: `TBD`

