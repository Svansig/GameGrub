# ARCH-022b - Extract Library Presentation Pipeline Use Case

- **ID**: `ARCH-022b`
- **Area**: `ui/model`, `domain/library`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - internal refactor
- **Reviewer**: `TBD`

## Problem

`onFilterApps` in `LibraryViewModel` is oversized and mixes policy, aggregation, sorting, and pagination concerns.

## Scope

- In scope:
  - Introduce `BuildLibraryPresentationUseCase`
  - Move badge/pagination/source-inclusion calculations to domain helpers
  - Keep ViewModel responsible for intent/state updates only
- Out of scope:
  - DAO/service boundary changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022c`, `ARCH-022e`
- Blocker (if `Blocked`): `ARCH-022a`

## Acceptance Criteria

- [ ] `onFilterApps` delegates core transformation work to a use case
- [ ] Pipeline behavior remains functionally equivalent
- [ ] Added/updated unit tests cover extraction seams

## Validation

- [ ] Targeted unit tests for extracted use case and ViewModel integration

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

