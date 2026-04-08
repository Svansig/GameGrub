# ARCH-039 - Implement RuntimeStore service for bundle registration and verification

- **ID**: `ARCH-039`
- **Area**: `runtime store`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`
- **Reviewer**: `TBD`

## Problem

We need an explicit service layer to manage bundle lifecycle (registration, verification, safe enumeration) without breaking existing code.

## Scope

- In scope:
  - Create `RuntimeStore` interface in `app.gamegrub.runtime.store`:
    - `registerBase(manifest: BaseManifest, rootfsPath: Path): Result<Unit>`
    - `registerRuntime(manifest: RuntimeManifest, runtimePath: Path): Result<Unit>`
    - `registerDriver(manifest: DriverManifest, driverPath: Path): Result<Unit>`
    - `registerProfile(manifest: LaunchProfileManifest): Result<Unit>`
    - `getBase(id: String): BaseManifest?`
    - `getRuntime(id: String): RuntimeManifest?`
    - `getDriver(id: String): DriverManifest?`
    - `getProfile(id: String): LaunchProfileManifest?`
    - `listBases(): List<BaseManifest>`
    - `listRuntimes(): List<RuntimeManifest>`
    - `listDrivers(): List<DriverManifest>`
    - `listProfiles(): List<LaunchProfileManifest>`
    - `verifyBundle(id: String, type: BundleType): Result<Unit>` (checks hash, directory structure, permissions)
  - Implement `RuntimeStoreImpl` with:
    - File-system based storage following ARCH-038 schema
    - Manifest serialization using ARCH-037 framework
    - Safe enumeration (handles missing/corrupt manifests gracefully)
    - Content hash verification (SHA256)
  - Create Hilt injection bindings
  - Unit tests for CRUD and verification
- Out of scope:
  - Installing bundles from external sources
  - Garbage collection (deferred)
  - Changing existing launch code

## Dependencies and Decomposition

- Parent ticket: `ARCH-038`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-042` (container store), `ARCH-043` (cache controller)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `RuntimeStore` interface created with all methods
- [ ] `RuntimeStoreImpl` created with file-system backend
- [ ] Bundle registration creates manifest and verifies directory structure
- [ ] Bundle verification checks SHA256 hash
- [ ] Safe enumeration handles missing/corrupt manifests without crashing
- [ ] Hilt bindings created and tested
- [ ] Unit tests cover:
  - [ ] Registration of each bundle type
  - [ ] Retrieval by ID
  - [ ] List operations (all bundles of a type)
  - [ ] Verification success and failure
  - [ ] Graceful handling of corrupt manifest files

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual registration and retrieval test works
- [ ] Corrupt manifest is skipped gracefully (with log warning)
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/runtime-store-schema.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

