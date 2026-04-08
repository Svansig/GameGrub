# Runtime Architecture Migration - Phases 1-11 Complete

> **Status**: Complete
> **Last Updated**: 2026-04-08

This document provides a comprehensive overview of the completed runtime architecture migration from shared mutable ImageFS to composed immutable bundles with explicit container isolation.

---

## Overview

The runtime architecture migration transformed the launch system from:
- **Before**: Single shared mutable `ImageFs` instance across all containers
- **After**: Composed immutable bundles with per-container isolation

This migration enables:
- Safe parallel container execution
- Deterministic cache keys per runtime configuration
- Graceful fallback to alternative runtimes/drivers
- Improved telemetry and failure recovery

---

## Phase Summary

| Phase | Focus | Key Components |
|-------|-------|----------------|
| Phase 0 | Discovery & Guardrails | Launch fingerprinting, failure taxonomy, milestone recording |
| Phase 1 | Manifest Foundations | Base/Runtime/Driver/Container/Cache manifests |
| Phase 2 | Runtime & Container Stores | RuntimeStore, ContainerStore |
| Phase 3 | Cache Controller | CacheController with key derivation and GC |
| Phase 4 | Session Assembler | SessionPlan, mount mappings, env-var models |
| Phase 5 | Graphics Cache Wiring | DXVK, VKD3D, Mesa shader cache adapters |
| Phase 6 | Launch Engine Integration | LaunchEngine abstraction, session execution |
| Phase 7 | Telemetry & Records | LaunchSessionRecord, local persistence |
| Phase 8 | Recommendations | CompatibilityRecord, LocalRecommendationResolver |
| Phase 9 | Adaptive Fallback | FallbackFailureClass, FallbackStateMachine |
| Phase 10 | Storage Policy & SD Support | StoragePolicy (Hot/Cold/Hybrid), split-root |
| Phase 11 | Migration Cleanup | Mutation points audit, architecture ready |

---

## Key Architecture Components

### Manifest Models (`app.gamegrub.runtime.manifest`, `app.gamegrub.container.manifest`)

```kotlin
// Base/Runtime manifests - immutable runtime bundles
BaseManifest(id, version, contentHash, rootfsPath)
RuntimeManifest(id, version, contentHash, runtimeType, binaryPath)
DriverManifest(id, version, contentHash, driverType, driverPath)

// Container/Cache manifests - per-container mutable state
ContainerManifest(id, gameId, platform, prefixPath, installPath, savesPath)
CacheManifest(id, cacheType, createdAt, baseId, runtimeId, driverId)
```

### Stores (`app.gamegrub.runtime.store`, `app.gamegrub.container.store`)

**RuntimeStore**: Manages immutable bundles
- `bases/`: Linux userspace skeleton
- `runtimes/`: Wine/Proton installations
- `drivers/`: Graphics drivers

**ContainerStore**: Manages per-container mutable state
- `prefix/`: Wine prefix
- `install/`: Game installation
- `saves/`: Save files
- `overrides/`: User configuration
- `cache/`: Container-specific cache

### Session Assembly (`app.gamegrub.session`)

**SessionPlan**: Launch-time composition of all required paths
- Base bundle paths
- Runtime bundle paths
- Driver bundle paths
- Container paths (prefix, install, saves)
- Mount mappings
- Environment variables

### Launch Engine (`app.gamegrub.launch`)

**LaunchEngine**: Executes game from SessionPlan
- Session validation
- Mount orchestration
- Process spawn
- Exit monitoring
- Failure detection

### Telemetry (`app.gamegrub.telemetry`)

**LaunchFingerprint**: Structured launch context
- Game ID, platform, store
- Runtime selection
- Driver selection
- Container config

**LaunchMilestone**: Time-stamped events during launch
- CONTAINER_RESOLVED
- RUNTIME_PREPARED
- DRIVER_LOADED
- GAME_STARTED
- GAME_EXITED

### Recommendations (`app.gamegrub.telemetry.recommendation`)

**CompatibilityRecord**: Historical launch data
- Success/failure count
- Last known good configuration
- Recommended fallback

**RecommendationResult**: Resolution output
- Primary configuration
- Fallback candidates

### Fallback (`app.gamegrub.fallback`)

**FailureClass**: Categorized failures
- PROCESS_SPAWN, BACKEND_INIT, GRAPHICS_INIT
- CONTAINER_SETUP, MISSING_DRIVER, CORRUPTED_CACHE

**FallbackStateMachine**: Adaptive retry policy
- Bounded retries with backoff
- Alternative runtime/driver selection

### Storage (`app.gamegrub.storage`)

**StoragePolicy**: Storage location strategy
- HotRuntime: Most data on device
- ColdBulk: Large data on external storage
- Hybrid: Split across both

---

## Directory Structure

```
{app_files_dir}/
├── runtime-store/              # Immutable bundles
│   ├── bases/{base-id}/
│   ├── runtimes/{runtime-id}/
│   └── drivers/{driver-id}/
├── containers/                 # Per-container mutable state
│   ├── {container-id}/
│   │   ├── prefix/
│   │   ├── install/
│   │   ├── saves/
│   │   ├── overrides/
│   │   └── cache/
├── cache/                      # Global caches
│   ├── shader/
│   ├── translator/
│   └── probe/
└── session/                    # Active session data
```

---

## Migration Status

### Complete

- [x] Manifest models with serialization
- [x] RuntimeStore with bundle registration/verification
- [x] ContainerStore with container lifecycle
- [x] CacheController with key derivation and GC
- [x] SessionAssembler for launch-time composition
- [x] Graphics cache adapters (DXVK, VKD3D, Mesa)
- [x] LaunchEngine for session execution
- [x] Launch records with local persistence
- [x] Recommendation resolvers
- [x] Fallback state machine
- [x] Storage policy support
- [x] Mutation points audit

### Remaining Work

- [ ] Update callers to use SessionPlan instead of ImageFs (follow-up tickets)
- [ ] Integration testing with live launch flows

---

## Documentation Reference

| Document | Phase | Description |
|----------|-------|-------------|
| `docs/runtime-launch-flow-current-state.md` | 0 | Current launch flow mapping |
| `docs/launch-failure-taxonomy.md` | 0 | Failure classification |
| `docs/runtime-store-directory-schema.md` | 2 | RuntimeStore schema |
| `docs/container-store-schema.md` | 2 | ContainerStore schema |
| `docs/cache-controller-design.md` | 3 | Cache design |
| `docs/mutation-points-audit.md` | 11 | Remaining mutations |

---

## Related Tickets

- **ARCH-030**: Inventory current runtime/container/imagefs launch flow
- **ARCH-031**: Add structured launch fingerprinting and telemetry hooks
- **ARCH-032**: Define launch failure taxonomy and recovery phases
- **ARCH-033**: Define milestones and structured outcome recording
- **ARCH-034**: BaseManifest & RuntimeManifest models
- **ARCH-035**: DriverManifest & LaunchProfileManifest models
- **ARCH-036**: ContainerManifest & CacheManifest models
- **ARCH-037**: Manifest serialization framework
- **ARCH-038**: RuntimeStore directory schema design
- **ARCH-039**: RuntimeStore service implementation
- **ARCH-040**: ContainerStore directory schema
- **ARCH-041**: ContainerStore service implementation
- **ARCH-042**: CacheController key derivation policy
- **ARCH-043**: CacheController service implementation
- **ARCH-044**: SessionPlan model
- **ARCH-045**: Mount/path mapping models
- **ARCH-046**: SessionAssembler service
- **ARCH-047**: Session artifact serialization
- **ARCH-048**: Graphics cache adapter abstraction
- **ARCH-049**: DXVK and VKD3D cache adapters
- **ARCH-050**: Mesa shader cache adapter
- **ARCH-051**: XDG cache and pre-launch directory creation
- **ARCH-052**: LaunchEngine abstraction
- **ARCH-053**: SessionPlan consumption in orchestrator
- **ARCH-054**: Env var and path mapping integration
- **ARCH-055**: Telemetry integration
- **ARCH-056**: LaunchSessionRecord schema
- **ARCH-057**: Launch records local persistence
- **ARCH-058**: Launch records read/query APIs
- **ARCH-059**: Compatibility record model
- **ARCH-060**: Recommendation result model
- **ARCH-061**: Local last-known-good resolver
- **ARCH-062**: Curated rules scaffolding
- **ARCH-063**: Failure classes for fallback
- **ARCH-064**: Fallback graph/state machine
- **ARCH-065**: Bounded retry policy
- **ARCH-066**: Storage policy model
- **ARCH-067**: Split-root container support
- **ARCH-068**: Mutation points audit
- **ARCH-069**: Architecture ready for migration

---

## Next Steps

1. **Integration**: Wire SessionAssembler and LaunchEngine into GameLaunchOrchestrator
2. **Migration**: Update ImageFs callers to use SessionPlan
3. **Testing**: Add integration tests for new architecture
4. **Verification**: Validate launch flows with new components
