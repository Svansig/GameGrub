# ARCH-022e - Extract Search and Compatibility Helpers

- **ID**: `ARCH-022e`
- **Area**: `ui/model`, `domain/library`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - helper extraction
- **Reviewer**: `TBD`

## Problem

Search normalization and compatibility mapping/fetch policies are embedded in `LibraryViewModel`.

## Scope

- In scope:
  - Extract query matcher helper
  - Extract compatibility status mapper + fetch policy use case
  - Add dedicated tests for mapping/filter interactions
- Out of scope:
  - API contract changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022b`
- Blocker (if `Blocked`): `ARCH-022b`

## Acceptance Criteria

- [ ] ViewModel delegates compatibility policy and mapping logic
- [ ] Search normalization helper is reusable and tested

## Validation

- [ ] Unit tests for compatibility mapping and query matching

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

