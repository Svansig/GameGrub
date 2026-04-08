# ARCH-043 - Implement CacheController service for cache lifecycle and retrieval

- **ID**: `ARCH-043`
- **Area**: `cache controller`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`
- **Reviewer**: `TBD`

## Problem

We need a service layer to attach caches by key, track usage, and enforce invalidation policies without manual management.

## Scope

- In scope:
  - Create `CacheController` interface in `app.gamegrub.cache.controller`:
    - `getOrCreateShaderCache(gameId: String, base: BaseManifest, runtime: RuntimeManifest, driver: DriverManifest): CacheHandle`
    - `getOrCreateProbeCache(base: BaseManifest, game: ContainerManifest): CacheHandle`
    - `getOrCreateTranslatorCache(runtime: RuntimeManifest, gameExeHash: String): CacheHandle`
    - `updateCacheMetadata(cacheId: String, lastUsedAt: Long): Result<Unit>`
    - `invalidateCache(cacheId: String, policy: InvalidationPolicy): Result<Unit>`
    - `listCaches(): List<CacheManifest>`
    - `garbageCollect(maxAgeDays: Int, minFreeBytes: Long): Result<GarbageCollectionReport>`
  - Implement `CacheControllerImpl` with:
    - Cache key derivation using ARCH-042 strategy
    - Directory creation under `/data/data/app.gamegrub/caches/{cache-id}/`
    - Manifest persistence
    - Safe garbage collection (no force deletion, just marked for cleanup)
  - Create `CacheHandle` data class (path, key, type)
  - Create Hilt bindings
  - Unit tests
- Out of scope:
  - Graphics component env-var wiring (Phase 6)
  - Actually enforcing garbage collection (deferred)
  - Changing existing code

## Dependencies and Decomposition

- Parent ticket: `ARCH-042`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-044` (session assembler), `ARCH-045` (graphics component wiring)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `CacheController` interface and `CacheControllerImpl` created
- [ ] `CacheHandle` data class created
- [ ] Cache directory creation follows deterministic naming
- [ ] get-or-create operations:
  - [ ] Reuse existing cache if key matches and not invalidated
  - [ ] Create new cache if key doesn't exist
  - [ ] Respect invalidation policies
- [ ] Metadata updates work (lastUsedAt timestamp)
- [ ] Garbage collection is safe (marks for cleanup, doesn't force delete)
- [ ] Hilt bindings created
- [ ] Unit tests cover:
  - [ ] Cache creation and retrieval
  - [ ] Key derivation and reuse
  - [ ] Metadata updates
  - [ ] Invalidation policies
  - [ ] Safe GC marking

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual cache creation test produces valid directory structure
- [ ] Same key retrieves same cache in subsequent calls
- [ ] Existing code is not broken
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/cache-controller-design.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

