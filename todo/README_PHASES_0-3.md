# GameGrub Runtime Architecture Migration - Phase 0-3 Backlog

**Status**: Ready for Phase 0 initiation
**Created**: 2026-04-07
**Total Tickets**: 14 (ARCH-030 through ARCH-043)
**Documentation**: 4 guides + 14 detailed tickets

## Quick Start

👉 **If you're implementing a ticket**: Start with [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)
👉 **If you're planning**: Read [`PHASES_0-3_OVERVIEW.md`](PHASES_0-3_OVERVIEW.md)
👉 **If you're new**: Start with [`NAVIGATION.md`](NAVIGATION.md)

## What Was Created

### 14 Actionable Tickets

**Phase 0: Discovery (4 tickets)**
- ARCH-030: Inventory launch flow
- ARCH-031: Fingerprinting infrastructure
- ARCH-032: Failure taxonomy
- ARCH-033: Milestone recording

**Phase 1: Manifests (4 tickets)**
- ARCH-034: Base/Runtime manifest models
- ARCH-035: Driver/Profile manifest models
- ARCH-036: Container/Cache manifest models
- ARCH-037: Serialization framework

**Phase 2: Stores (4 tickets)**
- ARCH-038: RuntimeStore schema design
- ARCH-039: RuntimeStore implementation
- ARCH-040: ContainerStore schema design
- ARCH-041: ContainerStore implementation

**Phase 3: Cache (2 tickets)**
- ARCH-042: Cache key derivation design
- ARCH-043: CacheController service

### 4 Comprehensive Guides

1. **NAVIGATION.md** (11 KB) - Quick links, glossary, key concepts
2. **EXECUTION_GUIDE.md** (9.4 KB) - Step-by-step how-to for implementers
3. **PHASES_0-3_OVERVIEW.md** (8.1 KB) - Phase 0-3 summary and dependencies
4. **PHASES_1-12_ROADMAP.md** (18 KB) - Full 12-phase vision

## Timeline

| Phase | Tickets | Weeks | Outputs |
|-------|---------|-------|---------|
| 0 | 4 | 2 | Telemetry + failure taxonomy |
| 1 | 4 | 2 | 6 manifest data classes |
| 2 | 4 | 3 | RuntimeStore + ContainerStore |
| 3 | 2 | 2 | CacheController |
| **Total** | **14** | **9 weeks** | **Foundation ready for Phase 4+** |

## Key Features

✅ **Non-Breaking** - All Phase 0-3 are safe, additive work
✅ **Small & Focused** - 1-2 weeks per ticket
✅ **Well-Documented** - Explicit dependencies, acceptance criteria, validation steps
✅ **Quality-Oriented** - Tests, linting, and review process defined
✅ **Executable** - Ready to assign and start immediately

## How to Pick Your First Ticket

1. Read [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md) (5 minutes)
2. Open [`ARCH-030.md`](ARCH-030.md) (the first ticket)
3. Mark status `In Progress` in the ticket file
4. Follow the step-by-step implementation instructions
5. When done, mark status `Done` and request review

## Architecture Vision

**From**: Shared mutable imagefs + implicit state + launch-time extraction
**To**: Immutable versioned bundles + explicit mutable containers + manifest-driven composition

After Phase 0-3, you'll have:
- Structured telemetry (fingerprints, milestones, failure taxonomy)
- 6 manifest data models
- RuntimeStore for bundle management
- ContainerStore for per-game state
- CacheController for keyed caches
- 100+ unit tests
- 3 new storage schemas
- Zero breaking changes
- Ready for Phase 4 (Session Assembler)

## Files in This Directory

```
Ticket Files:
  ARCH-030.md through ARCH-043.md     (14 tickets, 38.5 KB)

Guide Files:
  NAVIGATION.md                        (Quick links & glossary)
  EXECUTION_GUIDE.md                  (Implementation how-to)
  PHASES_0-3_OVERVIEW.md              (Phase 0-3 summary)
  PHASES_1-12_ROADMAP.md              (Full 12-phase vision)

Updated:
  INDEX.md                            (Master backlog with new section)
```

## Getting Help

| Question | Resource |
|----------|----------|
| "Where do I start?" | [`NAVIGATION.md`](NAVIGATION.md) |
| "How do I implement a ticket?" | [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md) |
| "What's the full scope?" | [`PHASES_1-12_ROADMAP.md`](PHASES_1-12_ROADMAP.md) |
| "What's the Kotlin style guide?" | See `AGENTS.md` (project root) |
| "How do I run tests?" | [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md) → Quick Reference |

## Validation Checklist

Before starting implementation:
- [ ] Read [`NAVIGATION.md`](NAVIGATION.md)
- [ ] Read [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)
- [ ] Read your assigned ticket file (ARCH-NNN.md)
- [ ] Understand dependencies (check "Parent" field in ticket)
- [ ] Review acceptance criteria (checkboxes in ticket)

## What's Next

1. **Immediately**: Review backlog with team, share [`NAVIGATION.md`](NAVIGATION.md)
2. **This week**: Assign ARCH-030 to first implementer
3. **Ongoing**: Track progress in [`INDEX.md`](INDEX.md), update ticket statuses
4. **After Phase 3**: Create Phase 4+ tickets based on completion

---

**Ready to start?** Pick [`ARCH-030.md`](ARCH-030.md) and follow [`EXECUTION_GUIDE.md`](EXECUTION_GUIDE.md)!

