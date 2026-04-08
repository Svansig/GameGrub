# GameGrub Runtime Architecture Migration - Phases 0-3 Ticket Backlog

**Created**: 2026-04-07
**Target Completion**: Staged by phase (0-3 foundation tickets)
**Status**: Initial backlog created

## Overview

This document outlines the first 14 tickets for refactoring GameNative into GameGrub with a composed-runtime architecture. The migration moves from a shared mutable imagefs/rootfs model to:

- **Immutable versioned bundles** (base Linux userspace, compatibility layer, graphics drivers)
- **Mutable per-game containers** (prefix, install, save, user-overrides)
- **Explicit keyed caches** (shader, translated code, probe)
- **Session-based launch composition** (manifest-driven assembly at launch time)
- **Structured telemetry and local fallback logic**

## Phase 0: Discovery and Guardrails (4 tickets)

**Goal**: Create visibility into current launch flow and establish measurement infrastructure.

| Ticket | Type | Purpose | Output |
|--------|------|---------|--------|
| [ARCH-030](ARCH-030.md) | Doc | Inventory launch paths and mutation points | `docs/runtime-launch-flow-current-state.md` |
| [ARCH-031](ARCH-031.md) | Feature | Add launch fingerprinting for debugging | `LaunchFingerprint` data class + hooks |
| [ARCH-032](ARCH-032.md) | Doc | Classify failure modes and recovery actions | `docs/launch-failure-taxonomy.md` |
| [ARCH-033](ARCH-033.md) | Feature | Record structured launch milestones | `LaunchMilestone` enum + `MilestoneRecorder` |

**Execution order**: ARCH-030 → ARCH-031 → ARCH-032 → ARCH-033
**Rationale**: Documentation first (no code risk), then instrumentation (safe, observable)

## Phase 1: Manifest Foundations (4 tickets)

**Goal**: Define Kotlin data models for all bundle and container manifests.

| Ticket | Type | Purpose | Output |
|--------|------|---------|--------|
| [ARCH-034](ARCH-034.md) | Code | Define base/runtime manifests | `BaseManifest`, `RuntimeManifest` classes |
| [ARCH-035](ARCH-035.md) | Code | Define driver/profile manifests | `DriverManifest`, `LaunchProfileManifest` classes |
| [ARCH-036](ARCH-036.md) | Code | Define container/cache manifests | `ContainerManifest`, `CacheManifest` classes |
| [ARCH-037](ARCH-037.md) | Code | Implement serialization/validation | `ManifestValidator`, `ManifestSerializer` interfaces |

**Execution order**: ARCH-034 → ARCH-035 → ARCH-036 → ARCH-037
**Rationale**: Simple types first, then complex interdependent types, then serialization framework
**Deliverables**:
- 6 manifest data classes with full validation
- Serialization/deserialization working for all
- Unit tests for JSON roundtrip and validation

## Phase 2: Runtime Store (4 tickets)

**Goal**: Create explicit storage and control planes for immutable bundles and mutable containers.

| Ticket | Type | Purpose | Output |
|--------|------|---------|--------|
| [ARCH-038](ARCH-038.md) | Design | Design new storage schema | `docs/runtime-store-schema.md` + `BundleRegistry` interface |
| [ARCH-039](ARCH-039.md) | Code | Implement bundle registration service | `RuntimeStore` interface + `RuntimeStoreImpl` |
| [ARCH-040](ARCH-040.md) | Design | Design container storage schema | `docs/container-store-schema.md` + `ContainerRegistry` interface |
| [ARCH-041](ARCH-041.md) | Code | Implement container lifecycle service | `ContainerStore` interface + `ContainerStoreImpl` |

**Execution order**: ARCH-038 → ARCH-039 → ARCH-040 → ARCH-041
**Rationale**: Design first (no risk), then implementation (depends on manifests from Phase 1)
**Deliverables**:
- RuntimeStore with bundle registration and verification
- ContainerStore with create/read/update/delete and path resolution
- Both backed by file-system with Hilt injection
- Directory schemas following best practices for immutability and permissions

## Phase 3: Cache Controller (2 tickets)

**Goal**: Make caches explicit, keyed by exact runtime/profile/game identity, and safely invalidatable.

| Ticket | Type | Purpose | Output |
|--------|------|---------|--------|
| [ARCH-042](ARCH-042.md) | Design | Design cache key derivation and invalidation | `docs/cache-controller-design.md` + `CacheKeyDeriver` interface |
| [ARCH-043](ARCH-043.md) | Code | Implement cache controller service | `CacheController` interface + `CacheControllerImpl` |

**Execution order**: ARCH-042 → ARCH-043
**Rationale**: Cache key strategy is foundational; implementation depends on it
**Deliverables**:
- Deterministic cache key derivation (SHA256 of base/runtime/driver/game identity)
- Cache creation and retrieval without manual management
- Metadata tracking (lastUsedAt)
- Safe garbage collection marking (not forced deletion)

## Key Dependencies and Sequencing

```
Phase 0:
  ARCH-030 (doc)
     ↓
  ARCH-031 (fingerprint) → ARCH-032 (taxonomy) → ARCH-033 (milestones)

Phase 1:
  ARCH-034 (base/runtime) → ARCH-035 (driver/profile)
                              ↓
                            ARCH-036 (container/cache)
                              ↓
                            ARCH-037 (serialization framework)

Phase 2:
  ARCH-038 (runtime store design) → ARCH-039 (runtime store impl)
  ARCH-040 (container store design) → ARCH-041 (container store impl)

Phase 3:
  ARCH-042 (cache design) → ARCH-043 (cache controller impl)

Cross-phase:
  Phase 1 must complete before Phase 2 (manifests needed for stores)
  Phase 2 must complete before Phase 3 (container store needed for cache association)
```

## Testing Strategy

- **Phase 0**: Purely observational; no behavior changes, only logging
- **Phase 1**: Unit tests for manifest construction, serialization, validation
- **Phase 2**: Unit tests for store CRUD operations, directory creation, manifest persistence
- **Phase 3**: Unit tests for key derivation, cache creation/retrieval, metadata tracking

## Integration Points (Do Not Break)

- **Existing launch code**: Must remain functional throughout
- **Existing container paths**: Will be mapped incrementally in Phase 12 (not in scope for 0-3)
- **Existing database**: Not modified by 0-3 (schema work is separate)
- **Existing service layer**: Not modified by 0-3 (service refactoring is separate)

## Definition of Done for Each Ticket

1. Code compiles without errors or warnings
2. Relevant unit/integration tests pass
3. `./gradlew lintKotlin` passes for touched files
4. Manual flow checks (if applicable) pass
5. No breaking changes to existing code
6. PR includes clear `Documentation Impact` statement
7. Implementation committed before review
8. Independent review completed
9. Post-review changes committed
10. Improvement opportunities logged in `docs/process-improvement-log.md`

## Risk Mitigation

**Risk**: Tickets too large to complete in one pass
**Mitigation**: Each ticket explicitly lists decomposition into child tickets if needed

**Risk**: Manifest format lock-in
**Mitigation**: Versioning built into serialization (ARCH-037); graceful fallback for version mismatches

**Risk**: Storage layout conflicts with existing code
**Mitigation**: New schemas use separate directory roots; no collision with legacy paths until Phase 12

**Risk**: Incorrect cache key derivation leads to unsafe reuse
**Mitigation**: Unit tests validate determinism; documentation explains invalidation policy

## Next Steps After Phase 3

After Phase 0-3 are complete, the foundation is in place for:

- **Phase 4**: Session Assembler (manifest-driven launch composition)
- **Phase 5**: Graphics cache environment variable wiring
- **Phase 6**: Launch Engine Integration (consuming session plans)
- **Phase 7+**: Telemetry records, recommendations, adaptive fallback, SD card support, old-path migration

## Links

- **User requirements**: Attached user request with full 12-phase plan
- **Architecture overview**: See AGENTS.md for project structure
- **Coding standards**: See AGENTS.md for Kotlin/Compose/Hilt guidelines
- **Related docs**:
  - `docs/runtime-launch-flow-current-state.md` (Phase 0 output)
  - `docs/runtime-store-schema.md` (Phase 2 output)
  - `docs/container-store-schema.md` (Phase 2 output)
  - `docs/cache-controller-design.md` (Phase 3 output)

---

**Status**: Ready for Phase 0 initiation
**Owner**: Architecture team
**Reviewer**: Lead engineer + architecture council

