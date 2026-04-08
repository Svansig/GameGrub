# ARCH-036 - Define ContainerManifest and CacheManifest data models

- **ID**: `ARCH-036`
- **Area**: `manifest + container store`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`
- **Reviewer**: `TBD`

## Problem

Mutable container state needs explicit tracking of where a container's prefix/install/save/cache directories are, what bundles it's using, and its creation/last-used metadata. Cache entries need explicit versioning and invalidation policies.

## Scope

- In scope:
  - Create `ContainerManifest` data class in `app.gamegrub.container.manifest`:
    - id (e.g., "steam_2213720_container_v1")
    - gameTitle (human-readable)
    - gameId (platform ID: steam app ID, GOG ID, etc.)
    - gamePlatform (enum: STEAM, GOG, EPIC, AMAZON)
    - profileId (reference to LaunchProfileManifest used)
    - containerPath (directory root where prefix/install/save live)
    - prefixPath (relative to containerPath)
    - installPath (relative to containerPath)
    - savePath (relative to containerPath)
    - userOverridesPath (relative to containerPath, for user-specific tweaks)
    - createdAt (timestamp)
    - lastUsedAt (timestamp)
    - size (in bytes, updated periodically)
  - Create `CacheManifest` data class in `app.gamegrub.cache.manifest`:
    - id (SHA256 hash of base/runtime/driver/profile/game-id tuple)
    - cacheType (enum: SHADER, TRANSLATED_CODE, PROBE, TRANSLATOR_METADATA)
    - cachePath (directory containing cache)
    - baseId (reference to BaseManifest)
    - runtimeId (reference to RuntimeManifest)
    - driverId (reference to DriverManifest)
    - gameId (nullable; if null, shared across games)
    - createdAt (timestamp)
    - lastUsedAt (timestamp)
    - size (in bytes)
    - invalidationPolicy (enum: NEVER_INVALIDATE, INVALIDATE_ON_RUNTIME_CHANGE, INVALIDATE_MANUALLY, INVALIDATE_ON_DATE)
  - Implement serialization for both
  - Add validation and cache key derivation
  - Unit tests
- Out of scope:
  - Persistence (ARCH-037)
  - Cache location policy (Phase 11)

## Dependencies and Decomposition

- Parent ticket: `ARCH-035`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-037` (manifest serialization), `ARCH-039` (container store)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `ContainerManifest` data class created with validation
- [ ] `CacheManifest` data class created with validation and key derivation
- [ ] Serialization/deserialization works for both
- [ ] Cache key derivation is deterministic and stable
- [ ] Unit tests cover:
  - [ ] Valid manifest construction
  - [ ] Invalid manifests fail validation
  - [ ] JSON serialization roundtrip
  - [ ] Cache key derivation (same inputs → same key)
  - [ ] Invalidation policy encoding

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual test shows deterministic cache key derivation
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`

