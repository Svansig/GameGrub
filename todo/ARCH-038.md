# ARCH-038 - Design RuntimeStore directory schema and scaffolding

- **ID**: `ARCH-038`
- **Area**: `runtime store + storage`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/runtime-store-schema.md with directory layout, manifest locations, and migration notes.`
- **Reviewer**: `TBD`

## Problem

We need a new storage layout for immutable bundles (base, runtime, driver) that is deterministic, versioned, and doesn't interfere with existing app state. We also need a clear migration path from legacy shared imagefs.

## Scope

- In scope:
  - Design and document the new directory schema under app-private storage:
    - `/data/data/app.gamegrub/bundles/bases/{base-id}/manifest.json`
    - `/data/data/app.gamegrub/bundles/bases/{base-id}/rootfs/` (immutable)
    - `/data/data/app.gamegrub/bundles/runtimes/{runtime-id}/manifest.json`
    - `/data/data/app.gamegrub/bundles/runtimes/{runtime-id}/` (immutable)
    - `/data/data/app.gamegrub/bundles/drivers/{driver-id}/manifest.json`
    - `/data/data/app.gamegrub/bundles/drivers/{driver-id}/` (immutable)
    - `/data/data/app.gamegrub/bundles/profiles/{profile-id}/manifest.json` (shared profiles)
  - Document file permissions strategy (read-only for bundles, except during install)
  - Document garbage collection assumptions
  - Create comprehensive migration guide for old imagefs references
  - Create a `BundleRegistry` interface for enumerating installed bundles
- Out of scope:
  - Implementing the store service (ARCH-039)
  - Installing bundles (deferred to per-bundle implementation)
  - Migrating existing imagefs (Phase 12)

## Dependencies and Decomposition

- Parent ticket: `ARCH-037`
- Child tickets: `ARCH-039`, `ARCH-040`, `ARCH-041`
- Related follow-ups: `SRV-036` (migrating service paths in Phase 12)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Documentation created at `docs/runtime-store-schema.md` with:
  - [ ] Complete directory tree and file layout
  - [ ] Manifest location rules
  - [ ] File permission conventions
  - [ ] Garbage collection policy (when bundles can be deleted)
  - [ ] Reference to content hash verification
- [ ] `BundleRegistry` interface designed with:
  - [ ] listBases(): List<BaseManifest>
  - [ ] listRuntimes(): List<RuntimeManifest>
  - [ ] listDrivers(): List<DriverManifest>
  - [ ] getBundle(id: String): Manifest?
  - [ ] verifyBundle(id: String): Result<Unit>
- [ ] Migration guide documented (not implemented yet)

## Validation

- [ ] Second reviewer validates schema against real-world bundle sizes
- [ ] Schema supports versioning of future bundles
- [ ] Documentation is clear and covers edge cases
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/runtime-store-schema.md` (to be created)
- Related PR: `TBD`
- Related commit(s): `TBD`

