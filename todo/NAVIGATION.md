# GameGrub Runtime Architecture Migration - Navigation and Quick Links

**Status**: Phase 0-3 backlog created and ready for execution
**Created**: 2026-04-07
**Maintainer**: Architecture team

## For Different Audiences

### 👷 I'm Implementing a Ticket

**Start here**: [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)

This guide walks you through:
- How to pick the next ticket
- Step-by-step implementation process
- Testing and validation checklist
- How to handle blockers
- Definition of done

**Then read**: The specific ticket file (e.g., `ARCH-030.md`) for details.

### 🏗️ I'm Planning Phase N

**Start here**: [`PHASES_1-12_ROADMAP.md`](PHASES_1-12_ROADMAP.md)

This roadmap covers:
- All 12 phases with goals and key concepts
- Estimated effort and timeline
- Risk mitigation strategies
- Testing strategy per phase
- Success criteria

**For Phases 0-3 details**: [`PHASES_0-3_OVERVIEW.md`](PHASES_0-3_OVERVIEW.md)

### 🔍 I'm Reviewing a PR

**Check**:
1. Does the ticket status match the work? (In Progress → Done)
2. Are all acceptance criteria in the ticket marked ✅?
3. Did `./gradlew lintKotlin` pass?
4. Are there unit tests? Do they pass?
5. Is there a `Documentation Impact` statement?
6. Is any breaking change clearly justified?

See [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md) → **Step 7: Request Review** for reviewer checklist.

### 📊 I'm Tracking Progress

**Master index**: [`INDEX.md`](INDEX.md)

This file maintains:
- Quick status of all tickets across all areas
- Priority and file location
- Done/In Progress/Backlog sections

Search for `ARCH-0` to see runtime migration tickets.

### 🎓 I'm New to the Project

**Prerequisites**:
1. Read `AGENTS.md` (project structure, build commands, coding standards)
2. Read `ARCHITECTURE.md` (current architecture overview)
3. Read [`PHASES_1-12_ROADMAP.md`](PHASES_1-12_ROADMAP.md) → Section "Architecture Vision"
4. Read the first few tickets to understand the pattern

**Then pick a ticket**:
- If you want to contribute immediately: Start with Phase 0 tickets (non-breaking, observational)
- If you want to understand the architecture first: Read all three overview docs

---

## Phase 0-3 Tickets (Ordered by Dependency)

### Phase 0: Discovery (2 weeks)
| ID | Status | Title | File |
|---|---|---|---|
| ARCH-030 | Backlog | Inventory current runtime/container/imagefs launch flow | `ARCH-030.md` |
| ARCH-031 | Backlog | Add structured launch fingerprinting and telemetry hooks | `ARCH-031.md` |
| ARCH-032 | Backlog | Define launch failure taxonomy and recovery phases | `ARCH-032.md` |
| ARCH-033 | Backlog | Define milestones and structured outcome recording for launches | `ARCH-033.md` |

### Phase 1: Manifests (2 weeks)
| ID | Status | Title | File |
|---|---|---|---|
| ARCH-034 | Backlog | Define BaseManifest and RuntimeManifest data models | `ARCH-034.md` |
| ARCH-035 | Backlog | Define DriverManifest and LaunchProfileManifest data models | `ARCH-035.md` |
| ARCH-036 | Backlog | Define ContainerManifest and CacheManifest data models | `ARCH-036.md` |
| ARCH-037 | Backlog | Implement manifest serialization and validation framework | `ARCH-037.md` |

### Phase 2: Stores (3 weeks)
| ID | Status | Title | File |
|---|---|---|---|
| ARCH-038 | Backlog | Design RuntimeStore directory schema and scaffolding | `ARCH-038.md` |
| ARCH-039 | Backlog | Implement RuntimeStore service for bundle registration and verification | `ARCH-039.md` |
| ARCH-040 | Backlog | Design ContainerStore directory schema | `ARCH-040.md` |
| ARCH-041 | Backlog | Implement ContainerStore service for container lifecycle management | `ARCH-041.md` |

### Phase 3: Cache (2 weeks)
| ID | Status | Title | File |
|---|---|---|---|
| ARCH-042 | Backlog | Design CacheController key derivation and invalidation policy | `ARCH-042.md` |
| ARCH-043 | Backlog | Implement CacheController service for cache lifecycle and retrieval | `ARCH-043.md` |

---

## Key Concepts Glossary

### Bundle (Immutable Component)
A versioned, read-only package of runtime code or supporting files:
- **Base**: Linux userspace skeleton (libc, system libs, basic tools)
- **Runtime**: Wine, Proton, or compatibility layer translator
- **Driver**: Graphics userspace (Vulkan, Mesa, DXVK, VKD3D)

### Container (Mutable Per-Game State)
Game-specific, writable storage for:
- **Prefix**: Wine prefix (C:/, registry, user data)
- **Install**: Game executable and data
- **Save**: Game save files
- **User Overrides**: Per-game tweaks

### Cache (Disposable, Keyed)
Automatically managed, clearable storage:
- **Shader Cache**: Compiled shaders (Mesa, DXVK)
- **Translated Code**: JIT'd or compiled bytecode (translator output)
- **Probe Cache**: Game probing results (features, capabilities)

### Session (Ephemeral Composition)
Launch-time assembly of:
- Selected base/runtime/driver/profile
- Mounted container directories
- Resolved cache paths
- Environment variables

### Manifest (Serializable Metadata)
JSON/Kotlin data class describing a component's identity:
- What it is (base-id, version, type)
- Where it is (relative path)
- What it contains (content hash for verification)
- How to use it (profile metadata, env vars)

---

## Directory Structure: New Schema

```
/data/data/app.gamegrub/
├── bundles/                    (Phase 2 output)
│   ├── bases/{base-id}/
│   │   ├── manifest.json
│   │   └── rootfs/             (immutable)
│   ├── runtimes/{runtime-id}/
│   │   ├── manifest.json
│   │   └── (wine/proton content)
│   ├── drivers/{driver-id}/
│   │   ├── manifest.json
│   │   └── (graphics userspace)
│   └── profiles/{profile-id}/
│       └── manifest.json
│
├── containers/                 (Phase 2 output)
│   └── {container-id}/
│       ├── manifest.json
│       ├── prefix/             (mutable)
│       ├── install/            (mutable)
│       ├── save/               (mutable)
│       └── user-overrides/     (mutable)
│
├── caches/                     (Phase 3 output)
│   └── {cache-key}/
│       ├── manifest.json
│       └── (shader/probe/code cache data)
│
└── telemetry/                  (Phase 0 output)
    └── sessions/
        └── {session-id}.json
```

---

## Dependencies Tree

```
Phase 0 (Instrumentation)
  ├─ ARCH-030 ────┐
  │                ├─→ ARCH-031 ──→ ARCH-032 ──→ ARCH-033
  │                └─────────────────────────────┘
  │
  └─ Creates foundation: LaunchFingerprint, MilestoneRecorder, FailureClass
                        ↓
Phase 1 (Manifests)
  └─ ARCH-034 ──→ ARCH-035 ──→ ARCH-036 ──→ ARCH-037
                                           ↓
Phase 2 (Stores)
  ├─ ARCH-038 ──→ ARCH-039
  ├─ ARCH-040 ──→ ARCH-041
  └─ Creates foundation: RuntimeStore, ContainerStore
                        ↓
Phase 3 (Cache)
  └─ ARCH-042 ──→ ARCH-043
                  ↓
Phase 4+ (Future)
  └─ SessionAssembler, LaunchEngine, Recommendations, Fallback, etc.
```

---

## Execution Timeline (Estimated)

| Week | Phase | Tickets | Key Milestone |
|------|-------|---------|---------------|
| 1-2 | 0 | ARCH-030 - ARCH-033 | Launch visibility + instrumentation |
| 3-4 | 1 | ARCH-034 - ARCH-037 | Manifest data classes + serialization |
| 5-7 | 2 | ARCH-038 - ARCH-041 | RuntimeStore + ContainerStore |
| 8-9 | 3 | ARCH-042 - ARCH-043 | CacheController |
| **9 weeks total** | | | Foundation ready for Phase 4+ |

---

## How to Contribute

### Picking Your First Ticket

1. Read [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)
2. Check dependencies: All Phase 0 tickets must be done before Phase 1
3. Pick the oldest uncompleted ticket (highest in the dependency tree)
4. Mark it `In Progress` in the ticket file
5. Open a draft PR as you work

### During Implementation

- Keep commits small and focused
- Run `./gradlew lintKotlin` before submitting
- Write unit tests (see `AGENTS.md` for testing guidelines)
- Add `Documentation Impact` statement to PR
- Link the ticket in your PR description

### For Review

- One colleague must review before merge
- Verify all acceptance criteria are met
- Check that `./gradlew testDebugUnitTest` passes
- Confirm no breaking changes to existing code

### After Merge

- Mark ticket `Status: Done`
- Update `todo/INDEX.md` to reflect completion
- Log any improvements to `docs/process-improvement-log.md`

---

## Documentation Produced by Phase 0-3

| Phase | Documents Created |
|-------|-------------------|
| 0 | `docs/runtime-launch-flow-current-state.md`, `docs/launch-failure-taxonomy.md` |
| 1 | (None required) |
| 2 | `docs/runtime-store-schema.md`, `docs/container-store-schema.md` |
| 3 | `docs/cache-controller-design.md` |

All docs are listed as "Output" in each ticket and in Acceptance Criteria.

---

## Questions?

### How do I know if a ticket is ready to start?
Check `Dependencies` in the ticket file. If all parent/prerequisite tickets are `Done`, you're good.

### What if I find a bug in an existing ticket?
Update the ticket with the bug info and create a PR to fix it. The original ticket owner (or reviewer) will approve.

### Can I work on multiple tickets in parallel?
No. Complete one ticket end-to-end (including review) before picking the next. This ensures dependencies are always satisfied and progress is clear.

### What if Phase 0-3 takes longer than 9 weeks?
That's fine. Each ticket is independent once its prerequisites are done. Progress will still be visible in `todo/INDEX.md` and commit history.

### How do I know when to start Phase 4?
Only after all 14 Phase 0-3 tickets are marked `Done` in `todo/INDEX.md`. Phase 4 will have its own set of tickets created at that time.

---

## Files Created by This Effort

```
todo/
├── ARCH-030.md through ARCH-043.md  (14 ticket files)
├── PHASES_0-3_OVERVIEW.md           (Phase 0-3 summary)
├── PHASES_1-12_ROADMAP.md           (Full vision)
├── EXECUTION_GUIDE.md               (How to work)
├── NAVIGATION.md                    (This file)
└── INDEX.md                         (Updated with ARCH-030-043 section)
```

---

**Ready to start?** Pick up [`ARCH-030.md`](ARCH-030.md) and follow the [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)!

