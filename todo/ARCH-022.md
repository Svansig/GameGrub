# ARCH-022 - Refactor LibraryViewModel Into Unified Gateway + UseCase Pipeline

- **ID**: `ARCH-022`
- **Area**: `ui/model`
- **Priority**: `P1`
- **Status**: `In Progress`
- **Owner**: `TBD`
- **Documentation Impact**: `Updated` - Added phased child-ticket plan for incremental refactor delivery
- **Reviewer**: `TBD`

## Problem

`LibraryViewModel` is still a monolith that mixes:
- direct DAO/service access,
- cross-store filtering and aggregation,
- auth/refresh orchestration,
- compatibility mapping/search helpers,
inside one class.

## Scope

- In scope:
  - Migrate direct data/service dependencies behind unified gateways
  - Extract filtering/aggregation pipeline into domain use case(s)
  - Move refresh/auth orchestration out of ViewModel
  - Isolate reusable policy/mapping helpers into domain packages
- Out of scope:
  - Other ViewModels

## Dependencies and Decomposition

- Parent ticket: `ARCH-008`
- Child tickets:
  - `ARCH-022a` - Extract ownership/type/install policy helpers (started)
  - `ARCH-022b` - Split `onFilterApps` pipeline into presentation use case + policies
  - `ARCH-022c` - Replace direct DAO collectors with unified `LibraryGateway`
  - `ARCH-022d` - Extract refresh + OAuth orchestration to use case/coordinator
  - `ARCH-022e` - Extract search/compatibility helpers and expand tests
- Related follow-ups: `TBD`
- Blocker (if `Blocked`): `ARCH-001c`

## Acceptance Criteria

- [ ] `LibraryViewModel` depends on unified gateway/use-case boundaries, not store-specific DAOs/services
- [ ] Filtering/aggregation logic is testable outside `LibraryViewModel`
- [ ] Refresh/auth orchestration is delegated to domain/use-case layer
- [ ] Existing behavior for all supported stores remains intact

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

## Progress Notes

- 2026-04-06: Broke down `ARCH-022` into child tickets (`ARCH-022a`..`ARCH-022e`).
- 2026-04-06: Completed `ARCH-022a` by extracting owner/type/install policy helpers to
  `app/src/main/java/app/gamegrub/domain/library/policy/LibraryOwnershipPolicy.kt` and
  validating `LibraryViewModelOwnerFilterTest`.

