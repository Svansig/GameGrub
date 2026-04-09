# ARCH-018 - Migrate Services to Use GameStoreService Base

- **ID**: `ARCH-018`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `Sisyphus`
- **Documentation Impact**: `No doc changes required` - Internal refactor

## Implementation (2026-04-09)

All three services already declared `GameStoreService` as their base, but were re-implementing the boilerplate it was meant to eliminate. Changes made:

**GameStoreService (base):**
- Made `performSync` a `suspend` function (removes need for `runBlocking` in subclasses)
- Added `null` action handling in `handleStartCommand` (sticky restart with throttle check)

**GOGService, EpicService, AmazonService:**
- Removed companion-object `isSyncInProgress` — base class `syncInProgress` field used instead
- Removed companion-object `lastSyncTimestampMs` / `hasPerformedInitialSyncFlag` (GOG) — base class fields used instead
- Removed private `scope` from GOGService and EpicService — `serviceScope` from base used instead
- Download jobs now launched on `serviceScope` (cancelled on `onDestroy` via base class)
- `onStartCommand` simplified to `startForeground` + `handleStartCommand`
- `onDestroy` no longer manually cancels `backgroundSyncJob` or scope (base handles)
- `performSync` implementations no longer manage sync state or call `runBlocking`
- AmazonService: inlined `syncLibrary()` helper into `performSync`

## Problem

Services still use duplicate boilerplate code.

## Scope

- In scope:
  - Refactor GOGService to extend GameStoreService
  - Refactor EpicService to extend GameStoreService
  - Refactor AmazonService to extend GameStoreService
  - Move store-specific logic to managers
- Out of scope:
  - UI changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-002a`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] All 3 services extend GameStoreService
- [x] Service code reduced by 30%
- [x] All functionality preserved

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin`
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
