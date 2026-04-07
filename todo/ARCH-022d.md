# ARCH-022d - Extract Refresh and OAuth Orchestration

- **ID**: `ARCH-022d`
- **Area**: `ui/model`, `domain/usecase`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - orchestration refactor
- **Reviewer**: `TBD`

## Problem

`LibraryViewModel` directly coordinates refresh and store-specific OAuth completion flows.

## Scope

- In scope:
  - Add unified refresh orchestration use case (gateway-driven)
  - Add unified OAuth callback coordinator keyed by `GameSource`
  - Reduce direct service calls in ViewModel
- Out of scope:
  - UI redesign of auth messaging

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022c`
- Blocker (if `Blocked`): `ARCH-022c`

## Acceptance Criteria

- [x] ViewModel delegates refresh and OAuth orchestration to use-case/coordinator layer
- [x] Auth state + sync behavior remains equivalent

## Validation

- [x] `./gradlew :app:testDebugUnitTest --tests "app.gamegrub.ui.model.LibraryViewModelOwnerFilterTest" --tests "app.gamegrub.domain.library.search.LibraryQueryMatcherTest" --tests "app.gamegrub.domain.library.compatibility.CompatibilityStatusMapperTest" --tests "app.gamegrub.domain.usecase.RefreshLibraryOrchestrationUseCaseTest"`

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

## Progress Notes

- 2026-04-06: Added `RefreshLibraryOrchestrationUseCase` and
  `CompleteLibraryOAuthUseCase`.
- 2026-04-06: `LibraryViewModel` now delegates refresh/OAuth completion and reads
  auth state via `AuthStateGateway`.

