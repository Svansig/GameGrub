# ARCH-035 - Define DriverManifest and LaunchProfileManifest data models

- **ID**: `ARCH-035`
- **Area**: `manifest + runtime store`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`
- **Reviewer**: `TBD`

## Problem

Graphics driver bundles, launch profiles (Wine/Proton configuration presets), and container configurations need explicit schemas. Like base/runtime manifests, they must be versionable and validated.

## Scope

- In scope:
  - Create `DriverManifest` data class in `app.gamegrub.runtime.manifest`:
    - id (e.g., "turnip-merged-2024-03-01")
    - version (semver or date)
    - contentHash (SHA256)
    - createdAt (timestamp)
    - driverPath (relative to storage root)
    - driverType (enum: VULKAN, GLES, TURNIP, ADRENO, etc.)
    - minGlibcVersion (version string, if required)
  - Create `LaunchProfileManifest` data class in `app.gamegrub.container.manifest`:
    - id (e.g., "wine-esync-modern")
    - profileName (human-readable)
    - baseId (reference to BaseManifest)
    - runtimeId (reference to RuntimeManifest)
    - driverId (reference to DriverManifest)
    - environmentVariables (key-value pairs for Wine/Proton settings)
    - launchArgs (default command-line arguments)
    - metadata (esync enabled, fsync enabled, DXVK HUD settings, etc.)
  - Implement serialization for both
  - Add validation methods
  - Unit tests
- Out of scope:
  - Container manifests (covered in ARCH-036)
  - Manifest persistence (covered in ARCH-037)

## Dependencies and Decomposition

- Parent ticket: `ARCH-034`
- Child tickets: `ARCH-036`
- Related follow-ups: `ARCH-037`, `ARCH-039`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `DriverManifest` data class created with validation
- [ ] `LaunchProfileManifest` data class created with validation
- [ ] Serialization/deserialization works for both
- [ ] Unit tests cover:
  - [ ] Valid manifest construction
  - [ ] Invalid manifests fail validation
  - [ ] JSON serialization roundtrip
  - [ ] Environment variable handling
  - [ ] Reference validation (can reference existing base/runtime/driver IDs)

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual test serialization produces valid JSON
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`

