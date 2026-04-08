# GameGrub 12-Phase Architecture Migration Roadmap

**Status**: Phase 0-3 tickets created (initial foundation)
**Created**: 2026-04-07
**Estimated Timeline**: Staged execution; expect Phases 0-3 to establish foundation by mid-2026

## Overview

This document provides the complete 12-phase roadmap for transforming GameNative into GameGrub with a composed-runtime architecture. Each phase has explicit goals, dependencies, and artifact requirements. Only Phases 0-3 have detailed tickets created initially; later phases will be decomposed into tickets as earlier phases complete.

## Architecture Vision

**Current State**: Shared mutable imagefs + per-container home/prefix + launch-time extraction/repair
**Target State**: Immutable versioned bundles + mutable per-game containers + manifest-driven session assembly + local fallback reasoning

### Key Design Principles

1. **Never mutate a bundle in place** - Runtime updates create new bundles, never patch old ones
2. **Cache reuse is keyed by exact identity** - Compute key from base/runtime/driver/profile/game; soft invalidation (new key) not destruction
3. **Session assembly is explicit** - Launch-time composition is manifest-driven, not implicit global state
4. **Fallback is reasoned** - Choose safe alternatives based on local success history, not blind retry
5. **Storage respects Android constraints** - Unrooted, app-private, limited internal storage; no required external dependencies for hot paths

## Phase 0: Discovery and Guardrails

**Goal**: Create visibility into current launch flow and establish measurement infrastructure
**Tickets**: 4
**Status**: Backlog (ready to start)

### ARCH-030: Inventory current runtime/container/imagefs launch flow
- **Type**: Documentation + code review
- **Output**: `docs/runtime-launch-flow-current-state.md`
- **Acceptance**: All entry points mapped, mutation points identified, failure modes listed

### ARCH-031: Add structured launch fingerprinting and telemetry hooks
- **Type**: Infrastructure (non-breaking)
- **Output**: `LaunchFingerprint` data class, telemetry hooks in launch paths
- **Acceptance**: Fingerprints recorded to disk; session IDs correlate with success/failure

### ARCH-032: Define launch failure taxonomy and recovery phases
- **Type**: Documentation + enums
- **Output**: `docs/launch-failure-taxonomy.md`, `FailureClass` enum, recovery action mapping
- **Acceptance**: All observed failure modes classified; detection strategy documented

### ARCH-033: Define milestones and structured outcome recording for launches
- **Type**: Infrastructure (non-breaking)
- **Output**: `LaunchMilestone` enum, `MilestoneRecorder` service
- **Acceptance**: Timelines recorded for process start, backend init, first frame

### Phase 0 Artifacts

- Visibility into current launch paths (no behavior changes)
- Structured telemetry collection (fingerprints, milestones)
- Failure classification taxonomy
- Foundation for fallback logic (Phase 10)

## Phase 1: Manifest Foundations

**Goal**: Define explicit Kotlin data models for bundles, containers, profiles, and caches
**Tickets**: 4
**Status**: Backlog (depends on Phase 0 discovery)

### ARCH-034: Define BaseManifest and RuntimeManifest data models
- **Type**: Code (data classes + validation)
- **Output**: `BaseManifest`, `RuntimeManifest` data classes
- **Acceptance**: Serialization roundtrip, validation catches invalid manifests

### ARCH-035: Define DriverManifest and LaunchProfileManifest data models
- **Type**: Code (data classes + validation)
- **Output**: `DriverManifest`, `LaunchProfileManifest` data classes
- **Acceptance**: Profile can reference base/runtime/driver IDs

### ARCH-036: Define ContainerManifest and CacheManifest data models
- **Type**: Code (data classes + validation)
- **Output**: `ContainerManifest`, `CacheManifest` data classes
- **Acceptance**: Container refs to profile; cache key derivation deterministic

### ARCH-037: Implement manifest serialization and validation framework
- **Type**: Code (framework)
- **Output**: `ManifestValidator`, `ManifestSerializer` interfaces + implementations
- **Acceptance**: Version tolerance, graceful error handling, schema evolution support

### Phase 1 Artifacts

- 6 manifest data classes (base, runtime, driver, profile, container, cache)
- Serialization/deserialization with version tolerance
- Validation framework for corrupt/missing files
- Foundation for Phase 2 stores

## Phase 2: Runtime Store and Container Store

**Goal**: Create explicit storage and control planes for immutable bundles and mutable containers
**Tickets**: 4
**Status**: Backlog (depends on Phase 1 manifests)

### ARCH-038: Design RuntimeStore directory schema and scaffolding
- **Type**: Design + documentation
- **Output**: `docs/runtime-store-schema.md`, `BundleRegistry` interface
- **Schema**:
  ```
  /data/data/app.gamegrub/bundles/
  ├── bases/{base-id}/
  │   ├── manifest.json
  │   └── rootfs/  (immutable)
  ├── runtimes/{runtime-id}/
  │   ├── manifest.json
  │   └── (wine/proton/translator content - immutable)
  ├── drivers/{driver-id}/
  │   ├── manifest.json
  │   └── (graphics userspace - immutable)
  └── profiles/{profile-id}/
      └── manifest.json  (shared across containers)
  ```
- **Acceptance**: Schema supports versioning, permissions enforced, migration guide clear

### ARCH-039: Implement RuntimeStore service for bundle registration and verification
- **Type**: Code (service)
- **Output**: `RuntimeStore` interface + `RuntimeStoreImpl`
- **Operations**: Register/get/list/verify bundles
- **Acceptance**: Bundle registration persists manifest, verification checks SHA256, enumeration is safe

### ARCH-040: Design ContainerStore directory schema
- **Type**: Design + documentation
- **Output**: `docs/container-store-schema.md`, `ContainerRegistry` interface
- **Schema**:
  ```
  /data/data/app.gamegrub/containers/
  └── {container-id}/
      ├── manifest.json
      ├── prefix/  (mutable Wine prefix)
      ├── install/  (mutable game install)
      ├── save/  (mutable game saves)
      └── user-overrides/  (per-game tweaks)
  ```
- **Acceptance**: Prefix seeding strategy documented, lifecycle clear

### ARCH-041: Implement ContainerStore service for container lifecycle management
- **Type**: Code (service)
- **Output**: `ContainerStore` interface + `ContainerStoreImpl`
- **Operations**: Create/read/update/delete containers, path resolution, prefix seeding
- **Acceptance**: Safe directory creation, manifest persistence, metadata tracking

### Phase 2 Artifacts

- RuntimeStore for immutable bundle management
- ContainerStore for mutable per-game state
- Clear separation of bundle and container storage
- Both backed by manifest-driven schema

## Phase 3: Cache Controller

**Goal**: Make caches explicit, keyed, and safely invalidatable
**Tickets**: 2
**Status**: Backlog (depends on Phase 2 stores)

### ARCH-042: Design CacheController key derivation and invalidation policy
- **Type**: Design + documentation
- **Output**: `docs/cache-controller-design.md`, `CacheKeyDeriver` interface
- **Key Derivation**:
  ```
  Shader cache:   SHA256(base-id | runtime-id | driver-id | game-exe-hash)
  Translator:     SHA256(runtime-id | game-exe-hash)
  Probe:          SHA256(base-id | game-exe-hash)
  ```
- **Invalidation Policies**:
  - NEVER_INVALIDATE (safe if components don't change)
  - INVALIDATE_ON_RUNTIME_CHANGE (clear when runtime version bumps)
  - INVALIDATE_ON_DATE (clear after N days)
  - INVALIDATE_MANUALLY (user explicit clear)
- **GC Policy**: Delete caches unused > N days or for uninstalled games
- **Acceptance**: Key derivation is deterministic, policies are documented

### ARCH-043: Implement CacheController service for cache lifecycle and retrieval
- **Type**: Code (service)
- **Output**: `CacheController` interface + `CacheControllerImpl`
- **Operations**: Get-or-create cache by key, update metadata, safe GC marking
- **Acceptance**: Same key reuses cache, invalidation policies respected, GC is non-destructive

### Phase 3 Artifacts

- Deterministic cache key derivation
- Cache controller service with get-or-create semantics
- Invalidation policy enforcement
- Safe garbage collection (marking, not deletion)

---

## Phase 4: Session Assembler

**Goal**: Represent launch-time composition explicitly instead of mutating shared runtime state
**Tickets**: 3 (estimated)
**Status**: Backlog (depends on Phase 3 cache controller)

### Key Concepts
- `SessionPlan`: Manifest for a launch (base/runtime/driver/container/caches + mounts + env vars)
- `SessionAssembler`: Service that resolves all components and produces a plan
- No launcher execution yet; just plan generation

### Deliverables
- SessionPlan data model (resolved components + mounts + env vars)
- SessionAssembler service (base/runtime/driver/container/cache resolution)
- Structured JSON output for inspection and testing
- Unit tests for assembly logic

---

## Phase 5: Cache Wiring for Graphics Components

**Goal**: Attach real cache paths for graphics userspace
**Tickets**: 2 (estimated)
**Status**: Backlog (depends on Phase 4 session assembler)

### Key Concepts
- Mesa shader cache (env: MESA_SHADER_CACHE_DIR)
- DXVK state cache (env: DXVK_STATE_CACHE_PATH)
- VKD3D shader cache (env: VKD3D_SHADER_CACHE_PATH)

### Deliverables
- Cache path resolution in session assembly
- Env var injection in launch environment
- Cache directory creation before process spawn
- Tests for cache path correctness

---

## Phase 6: Launch Engine Integration

**Goal**: Move launch execution onto session plans
**Tickets**: 3 (estimated)
**Status**: Backlog (depends on Phase 5 cache wiring)

### Key Concepts
- `LaunchEngine`: Service consuming SessionPlan for process execution
- Replace implicit global state with explicit plan
- Preserve current behavior behind compatibility flags

### Deliverables
- LaunchEngine service
- Integration with existing launchers
- Session-based telemetry (session ID in logs)
- Tests for plan-driven execution

---

## Phase 7: Telemetry and Compatibility Records

**Goal**: Persist structured launch outcomes for local reasoning
**Tickets**: 2 (estimated)
**Status**: Backlog (depends on Phase 6 launch engine)

### Key Concepts
- `LaunchSessionRecord`: Outcome persistence (success/failure, milestones, fallback path, configs used)
- Local-only analysis (no remote telemetry)
- Enables last-known-good and recommendation logic

### Deliverables
- LaunchSessionRecord data model
- Local persistence (append-only log)
- Queryable interface for recommendation logic
- Tests for record writing/reading

---

## Phase 8: Recommendation Foundations

**Goal**: Choose profiles/runtimes/drivers based on local facts
**Tickets**: 2 (estimated)
**Status**: Backlog (depends on Phase 7 telemetry)

### Key Concepts
- Local last-known-good resolver (find configs that worked before)
- Curated rules engine (expert-provided profiles for known titles)
- Confidence scoring (confidence: local history > rule > fallback)

### Deliverables
- Recommendation data model (profile + confidence + source)
- Local resolver (query launch records)
- Rules engine scaffolding
- Tests for recommendation logic

---

## Phase 9: Adaptive Fallback

**Goal**: Retry safe alternatives in bounded, explainable ways
**Tickets**: 3 (estimated)
**Status**: Backlog (depends on Phase 8 recommendations)

### Key Concepts
- `FailureClass` (from Phase 0) maps to fallback graph
- Non-destructive fallbacks first:
  - Alternate backend (Vulkan → GLES)
  - Alternate profile (modern Wine → conservative preset)
  - Launcher bypass (if Wine hangs, try direct Proton)
  - Graphics preset downgrade (if crash, disable advanced features)
- Bounded retry (max 3 attempts, log all attempts)

### Deliverables
- Fallback graph/state machine
- Bounded retry policy
- Non-destructive fallback implementations
- Tests for fallback precedence

---

## Phase 10: Storage Policy and SD Support

**Goal**: Support split storage roots cleanly without putting hot runtime state on SD
**Tickets**: 3 (estimated)
**Status**: Backlog (depends on Phase 9 fallback)

### Key Concepts
- Internal storage: app-private (bundles, caches, containers, runtime state)
- External storage: optional, for large cold install payloads only
- Storage availability checks (fail gracefully if external unavailable)
- Move flow (user can relocate game installs)

### Deliverables
- Storage policy model (internal vs external roots)
- Volume availability checks
- Move flow implementation
- Performance warnings for external placement

---

## Phase 11: Migration Off Old Shared Mutable Runtime

**Goal**: Gradually stop relying on old shared mutable runtime/imagefs
**Tickets**: 5 (estimated)
**Status**: Backlog (depends on Phase 10 storage policy)

### Key Concepts
- Identify all remaining shared-runtime mutations
- Replace with bundle/session composition
- Remove re-extraction paths
- Migrate imagefs references behind new stores

### Deliverables
- Migration checklist (all shared mutations identified)
- Replacement implementations for each mutation
- Re-extraction removal (safe, because bundles are stable)
- Documentation of new canonical paths

---

## Phase 12: Validation, Testing, and Go-Live

**Goal**: Comprehensive validation and safe rollout
**Tickets**: 4 (estimated)
**Status**: Backlog (depends on Phase 11 migration)

### Key Concepts
- Integration tests (all platforms, all stores)
- Performance baseline vs old path
- Regression tests (existing titles still work)
- Safe rollout (feature flag, canary, gradual rolldown)

### Deliverables
- Integration test suite
- Performance profiling (old vs new)
- Regression test coverage
- Rollout plan and monitoring

---

## Cross-Phase Dependencies

### Strict Ordering
```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6 → Phase 7 → Phase 8 → Phase 9 → Phase 10 → Phase 11 → Phase 12
```

### Parallel Work (After Phase 1)
- Once Phase 1 manifests are done, design work for Phase 2-3 can start before Phase 1 code is complete
- Once Phase 3 cache is done, Phase 4 session assembler design can start
- Documentation and test scaffolding can happen in parallel

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Tickets too large | Explicit decomposition in each ticket; split if > 1 week effort |
| Manifest format lock-in | Versioning in serialization; graceful fallback |
| Storage conflicts | New schemas are separate; no collision with legacy until Phase 11 |
| Cache safety | Deterministic keys + extensive unit tests |
| Runtime mutations | Phase 11 explicitly identifies and replaces all mutations |
| Rollout breaks | Comprehensive integration tests + feature flags + canary |

## Test Strategy

- **Phase 0-1**: Unit tests only (data classes, validation, serialization)
- **Phase 2-3**: Unit tests + local file-system tests (stores, keys)
- **Phase 4-5**: Unit tests + integration tests (assembly, paths)
- **Phase 6-7**: Unit tests + e2e tests (launch, record persistence)
- **Phase 8-11**: Unit + integration + regression tests (recommendations, fallback, migration)
- **Phase 12**: Full regression suite on all platforms

## Estimation

| Phase | Tickets | Estimated Effort | Dependencies |
|-------|---------|------------------|--------------|
| 0 | 4 | 2 weeks | None |
| 1 | 4 | 2 weeks | Phase 0 |
| 2 | 4 | 3 weeks | Phase 1 |
| 3 | 2 | 2 weeks | Phase 2 |
| 4 | 3 | 2 weeks | Phase 3 |
| 5 | 2 | 2 weeks | Phase 4 |
| 6 | 3 | 3 weeks | Phase 5 |
| 7 | 2 | 1 week | Phase 6 |
| 8 | 2 | 1 week | Phase 7 |
| 9 | 3 | 2 weeks | Phase 8 |
| 10 | 3 | 2 weeks | Phase 9 |
| 11 | 5 | 4 weeks | Phase 10 |
| 12 | 4 | 3 weeks | Phase 11 |

**Total Estimated**: ~32 weeks (parallel work possible after Phase 1)

## Success Criteria

✅ **Phase 0-3 Checkpoints**:
1. Launches record fingerprints with base/runtime/driver/container IDs
2. Manifest data classes serialize/deserialize correctly
3. RuntimeStore and ContainerStore can register and retrieve bundles/containers
4. Cache controller assigns deterministic keys
5. All code compiles with `./gradlew lintKotlin` passing
6. Unit tests pass with > 80% coverage on new code
7. No breaking changes to existing launch behavior
8. Documentation updated for all schemas and services

## Next Steps

1. **Immediate**: Start Phase 0 (ARCH-030 inventory, non-blocking)
2. **Week 1**: Complete Phase 0 discovery and fingerprinting
3. **Week 2-3**: Implement Phase 1 manifest data classes
4. **Week 4-5**: Implement Phase 2 stores (bundle + container)
5. **Week 6-7**: Implement Phase 3 cache controller
6. **Review & adjust**: Assess progress, gather feedback, create Phase 4+ tickets

## Related Documentation

- **AGENTS.md**: Project structure, build commands, coding standards
- **ARCHITECTURE.md**: Current high-level architecture
- **docs/runtime-launch-flow-current-state.md**: (Phase 0 output) Current launch paths
- **docs/runtime-store-schema.md**: (Phase 2 output) Bundle storage layout
- **docs/container-store-schema.md**: (Phase 2 output) Container storage layout
- **docs/cache-controller-design.md**: (Phase 3 output) Cache key strategy

---

**Roadmap Status**: Published for Phase 0-3 backlog creation
**Next Review**: After Phase 3 completion (estimated mid-2026)

