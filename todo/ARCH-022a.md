# ARCH-022a - Extract Library Ownership/Type Policy Helpers

- **ID**: `ARCH-022a`
- **Area**: `ui/model`, `domain/library`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - internal code organization
- **Reviewer**: `TBD`

## Problem

Ownership/type/install bypass helper functions are colocated in `LibraryViewModel`, making the ViewModel harder to reason about and reuse.

## Scope

- In scope:
  - Move pure helper functions to `app/src/main/java/app/gamegrub/domain/library/policy/`
  - Update ViewModel imports/usages
  - Keep behavior identical
  - Keep or adjust unit tests to validate moved helpers
- Out of scope:
  - Changing filtering behavior
  - Rewriting `onFilterApps`

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022b`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Helper functions live under `app.gamegrub.domain.library.policy`
- [x] `LibraryViewModel` references extracted helpers
- [x] Existing owner filter tests pass

## Validation

- [x] `./gradlew :app:testDebugUnitTest --tests "app.gamegrub.ui.model.LibraryViewModelOwnerFilterTest"`

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

## Progress Notes

- 2026-04-06: Moved owner/type/install helper functions into
  `app/src/main/java/app/gamegrub/domain/library/policy/LibraryOwnershipPolicy.kt`.
- 2026-04-06: Updated `LibraryViewModel` and owner-filter tests to use extracted policy helpers.

