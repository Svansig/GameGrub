# ARCH-008 - Unified Library ViewModel

- **ID**: `ARCH-008`
- **Area**: `ui/model`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

LibraryViewModel and MainViewModel have separate logic for each game store. This causes duplicated state management and UI state derivation.

## Scope

- In scope:
  - Create unified GameStoreLibraryViewModel
  - Use GameRepository instead of separate DAOs
  - Unify library filtering/sorting logic
  - Single source of truth for library state
- Out of scope:
  - Auth state changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-008a` - Analyze existing ViewModels
  - `ARCH-008b` - Create unified library state
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001c` (needs repository)

## Acceptance Criteria

- [ ] Unified library ViewModel with single state
- [ ] All stores use same ViewModel
- [ ] Filter/sort logic unified

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: View library for each store
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
