# ARCH-041 - Implement ContainerStore service for container lifecycle management

- **ID**: `ARCH-041`
- **Area**: `container store`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`
- **Reviewer**: `TBD`

## Problem

We need an explicit service to create, manage, and track mutable per-game containers without breaking existing code.

## Scope

- In scope:
  - Create `ContainerStore` interface in `app.gamegrub.container.store`:
    - `createContainer(manifest: ContainerManifest, profile: LaunchProfileManifest): Result<Unit>`
    - `getContainer(id: String): ContainerManifest?`
    - `listContainers(): List<ContainerManifest>`
    - `updateContainer(manifest: ContainerManifest): Result<Unit>` (metadata updates only)
    - `deleteContainer(id: String): Result<Unit>`
    - `getPrefixPath(containerId: String): Path`
    - `getInstallPath(containerId: String): Path`
    - `getSavePath(containerId: String): Path`
    - `seedPrefixFromTemplate(containerId: String, templatePath: Path): Result<Unit>`
  - Implement `ContainerStoreImpl` with:
    - File-system backend following ARCH-040 schema
    - Safe directory creation and initialization
    - Manifest persistence using ARCH-037
    - Prefix seeding logic (empty or from template)
  - Create Hilt bindings
  - Unit tests for all operations
- Out of scope:
  - Migrating existing containers (Phase 12)
  - Prefix template creation
  - Changing existing launch code

## Dependencies and Decomposition

- Parent ticket: `ARCH-040`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-043` (cache controller), `ARCH-044` (session assembler)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `ContainerStore` interface created with all methods
- [ ] `ContainerStoreImpl` created with file-system backend
- [ ] Container creation:
  - [ ] Manifest is validated
  - [ ] Directory structure is created
  - [ ] Prefix is seeded (empty or from template)
  - [ ] Manifest is persisted
- [ ] Container retrieval and listing work
- [ ] Metadata updates preserve existing data
- [ ] Hilt bindings created and tested
- [ ] Unit tests cover:
  - [ ] Create, read, update, delete operations
  - [ ] Directory structure validation
  - [ ] Prefix seeding
  - [ ] Path resolution (getPrefixPath, etc.)
  - [ ] Graceful error handling

## Validation

- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] Manual container creation and retrieval test works
- [ ] Existing code is not broken
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/container-store-schema.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

