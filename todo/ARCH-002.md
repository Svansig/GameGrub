# ARCH-002 - Unified Service Abstraction Layer

- **ID**: `ARCH-002`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each game store has a separate Android Service (SteamService, GOGService, EpicService, AmazonService) with nearly identical boilerplate (lifecycle, sync logic, notifications). This causes:
- Duplicated lifecycle management code
- Inconsistent behavior between stores
- Hard to add common features (background sync, battery optimization)

## Scope

- In scope:
  - Create abstract `GameStoreService` base class
  - Factor out common sync logic, lifecycle, notifications
  - Migrate all 4 services to extend base class
  - Keep store-specific logic in managers/domains
- Out of scope:
  - UI layer changes
  - Database schema changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-002a` - Define GameStoreService interface/base
  - `ARCH-002b` - Migrate GOGService to base
  - `ARCH-002c` - Migrate EpicService to base  
  - `ARCH-002d` - Migrate AmazonService to base
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001c` (needs unified game model for service layer)

## Acceptance Criteria

- [ ] Abstract `GameStoreService` with common lifecycle
- [ ] Common sync throttle logic
- [ ] Common notification handling
- [ ] All 4 services extend base class
- [ ] Store-specific logic moved to managers

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual flow - Start each service, verify lifecycle
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
