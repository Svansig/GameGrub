# ARCH-040 - Design ContainerStore directory schema

- **ID**: `ARCH-040`
- **Area**: `container store + storage`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/container-store-schema.md with directory layout and seeding strategy.`
- **Reviewer**: `TBD`

## Problem

Containers (per-game mutable state) need a clean schema separate from bundles. We must define the structure and seeding strategy for prefix/install/save directories.

## Scope

- In scope:
  - Design directory schema under app-private storage:
    - `/data/data/app.gamegrub/containers/{container-id}/manifest.json`
    - `/data/data/app.gamegrub/containers/{container-id}/prefix/` (mutable Wine prefix)
    - `/data/data/app.gamegrub/containers/{container-id}/install/` (mutable game install)
    - `/data/data/app.gamegrub/containers/{container-id}/save/` (mutable save files)
    - `/data/data/app.gamegrub/containers/{container-id}/user-overrides/` (per-game tweaks)
  - Define prefix seeding strategy:
    - Initial empty prefix
    - Optional "template prefix" from runtime bundle
    - Incremental patch for game-specific requirements
  - Document container lifecycle (creation, use, cleanup)
  - Design ContainerRegistry interface
- Out of scope:
  - Implementing the store service (ARCH-041)
  - Prefix template creation (deferred)
  - Existing container migration (Phase 12)

## Dependencies and Decomposition

- Parent ticket: `ARCH-038`
- Child tickets: `ARCH-041`
- Related follow-ups: `ARCH-042` (container store implementation)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Documentation created at `docs/container-store-schema.md` with:
  - [ ] Directory structure and purpose of each subdir
  - [ ] Manifest location and lifecycle
  - [ ] File ownership/permissions
  - [ ] Prefix seeding strategy (empty, template, patch)
  - [ ] Container naming convention
- [ ] `ContainerRegistry` interface designed with:
  - [ ] createContainer(manifest: ContainerManifest): Result<Unit>
  - [ ] getContainer(id: String): ContainerManifest?
  - [ ] listContainers(): List<ContainerManifest>
  - [ ] updateMetadata(id: String, lastUsedAt: Long): Result<Unit>
- [ ] Prefix seeding strategy documented (not implemented)

## Validation

- [ ] Second reviewer validates against real container sizes
- [ ] Design separates immutable bundles from mutable container state clearly
- [ ] Documentation is clear and comprehensive
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/container-store-schema.md` (to be created)
- Related PR: `TBD`
- Related commit(s): `TBD`

