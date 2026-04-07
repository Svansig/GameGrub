# ARCH-022c - Replace Direct DAO Collection With LibraryGateway

- **ID**: `ARCH-022c`
- **Area**: `ui/model`, `gateway`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - boundary refactor
- **Reviewer**: `TBD`

## Problem

`LibraryViewModel` collects store-specific DAO flows directly, violating unified data boundary goals.

## Scope

- In scope:
  - Consume aggregated library streams via `LibraryGateway`
  - Remove direct DAO collector wiring from `LibraryViewModel`
- Out of scope:
  - Full rewrite of filter logic

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022b`
- Blocker (if `Blocked`): `ARCH-022b`

## Acceptance Criteria

- [x] ViewModel no longer directly subscribes to per-store DAO streams
- [x] Equivalent merged library state still renders correctly

## Validation

- [x] `./gradlew :app:testDebugUnitTest --tests "app.gamegrub.ui.model.LibraryViewModelOwnerFilterTest" --tests "app.gamegrub.domain.library.search.LibraryQueryMatcherTest" --tests "app.gamegrub.domain.library.compatibility.CompatibilityStatusMapperTest" --tests "app.gamegrub.domain.usecase.RefreshLibraryOrchestrationUseCaseTest"`

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

## Progress Notes

- 2026-04-06: Added `LibraryGateway.observeSourceSnapshot()` and
  `LibrarySourceSnapshot` to unify per-store data observation.
- 2026-04-06: Replaced direct DAO collection in `LibraryViewModel.init` with
  a single `LibraryGateway` snapshot collector.

