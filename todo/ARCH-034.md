# ARCH-034 - Define BaseManifest and RuntimeManifest data models

- **ID**: `ARCH-034`
- **Area**: `manifest + runtime store`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only; manifest schema versioning documented in ARCHITECTURE.md later.`
- **Reviewer**: `TBD`

## Problem

There is no explicit representation of what a "base runtime" or "compatibility layer" bundle is. Before we can store and compose them, we need Kotlin data models that can be persisted, validated, and versioned.

## Scope

- In scope:
  - Create `BaseManifest` data class in `app.gamegrub.runtime.manifest`:
    - id (e.g., "base-linux-glibc2.35-2.35")
    - version (semver or hash)
    - contentHash (SHA256 of base bundle)
    - createdAt (timestamp)
    - rootfsPath (relative to storage root)
    - description (human-readable)
  - Create `RuntimeManifest` data class in `app.gamegrub.runtime.manifest`:
    - id (e.g., "wine-8.0-glibc2.35")
    - version (semver)
    - contentHash (SHA256 of runtime bundle)
    - createdAt (timestamp)
    - runtimePath (relative to storage root)
    - baseId (reference to compatible BaseManifest)
    - runtimeType (enum: WINE, PROTON, TRANSLATOR, etc.)
    - metadata (translator version, DXVK version, VKD3D version if embedded)
  - Implement serialization (using kotlinx-serialization or Moshi) for both
  - Add validation methods (check fields are non-empty, hash format is valid, etc.)
  - Unit tests for construction and serialization
- Out of scope:
  - Persistence (covered in ARCH-037)
  - Driver manifests (separate ticket ARCH-035)
  - Container manifests (separate ticket ARCH-036)

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `ARCH-035`, `ARCH-036`, `ARCH-037`
- Related follow-ups: `ARCH-039` (store implementation)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `BaseManifest` data class defined with all fields and validation
- [ ] `RuntimeManifest` data class defined with all fields and validation
- [ ] Serialization/deserialization works for both (roundtrip tests)
- [ ] Content hash computation or verification works (SHA256)
- [ ] Unit tests cover:
  - [ ] Valid manifest construction
  - [ ] Invalid manifests fail validation
  - [ ] JSON serialization roundtrip
  - [ ] Content hash validation
- [ ] No breaking changes to existing code

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual serialization test produces valid JSON
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `N/A` (schema details in manifest classes)
- Related PR: `TBD`
- Related commit(s): `TBD`

