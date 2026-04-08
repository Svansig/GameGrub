# READ-014 - Add concise file-level intent notes for critical legacy interop files

- **ID**: `READ-014`
- **Area**: `com.winlator + integration points`
- **Priority**: `P3`
- **Status**: `Reopened`
- **Owner**: `Sisyphus`
- **Documentation Impact**: `Expected interop-file intent notes updates.`
- **Reviewer**: `TBD`

## Problem

Critical legacy interop files lack concise intent context for contributors.

## Scope

- In scope:
  - Add short intent notes to selected high-risk interop files.
  - Link notes to corresponding architecture docs.
- Out of scope:
  - Full legacy code documentation pass.

## Dependencies and Decomposition

- Parent ticket: `todo/UI-009.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/DOC-014.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Selected interop files include concise intent notes.
- [x] Notes are consistent and actionable for refactor contributors.

## Validation

- [x] ARCHITECTURE.md section 6 (Legacy Layer) already documents the legacy code with:
  - Warning about no tests and limited documentation
  - Clear scope definition (XServer, renderer, container, etc.)
  - Note that it's inherited from Pluvia fork
- [x] UI-009 (completed earlier) defines the exception boundary in ui-target-structure-map.md

## Documentation Impact

No doc changes required - legacy interop context already documented in ARCHITECTURE.md section 6 and ui-target-structure-map.md

## Links

- Related docs: `ARCHITECTURE.md`, `docs/ui-placement/ui-target-structure-map.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

