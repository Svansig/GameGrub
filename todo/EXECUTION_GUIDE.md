# Phase 0-3 Execution Guide

**Created**: 2026-04-07
**Purpose**: How to begin and progress through Phases 0-3 of the runtime architecture migration

## Quick Start

### For the First Implementer

1. **Read these documents in order**:
   - `todo/PHASES_0-3_OVERVIEW.md` (this overview)
   - `todo/PHASES_1-12_ROADMAP.md` (full vision)
   - `todo/ARCH-030.md` (first ticket to start)

2. **Start with Phase 0 (ARCH-030-033)**:
   - These tickets are non-breaking (purely observational/instrumentation)
   - No risk to existing code
   - Must be done first (foundation for all later phases)

3. **Expected timeline for Phase 0-3**:
   - Phase 0: 2 weeks (inventory + instrumentation)
   - Phase 1: 2 weeks (data classes + validation)
   - Phase 2: 3 weeks (stores)
   - Phase 3: 2 weeks (cache controller)
   - **Total**: ~9 weeks if done sequentially

## Ticket Picking Workflow

### Rule 1: Always Respect Dependencies
From `PHASES_0-3_OVERVIEW.md`:
```
Phase 0:
  ARCH-030 → ARCH-031 → ARCH-032 → ARCH-033

Phase 1:
  ARCH-034 → ARCH-035 → ARCH-036 → ARCH-037

Phase 2:
  ARCH-038 → ARCH-039 → ARCH-040 → ARCH-041

Phase 3:
  ARCH-042 → ARCH-043
```

**Never start a ticket until all its dependencies are done.**

### Rule 2: Update Ticket Status as You Work

Each ticket has a **Status** field. Update it in the ticket file:
- `Status: Backlog` → `Status: In Progress` (when you start)
- `Status: In Progress` → `Status: Done` (when acceptance criteria met + review complete)

### Rule 3: One Ticket Per Agent/Person

Don't pick a new ticket until the current one is marked `Done` (implementation + review + post-review commits complete).

## Execution Steps for Each Ticket

### 1. Read the Ticket

Open the ticket file (e.g., `todo/ARCH-030.md`). Understand:
- **Problem**: What is broken or missing?
- **Scope**: What's in/out of scope?
- **Dependencies**: What must be done first?
- **Acceptance Criteria**: How do I know when I'm done?

### 2. Mark as In Progress

Update the ticket file:
```markdown
- **Status**: `In Progress`
- **Owner**: `<your name>`
```

### 3. Do the Work

- Write code, docs, or tests as specified
- Follow the coding guidelines in `AGENTS.md`
- Make sure each commit message is clear
- Don't expand scope (if you discover new work, create a child ticket)

### 4. Verify Acceptance Criteria

Before marking done, check all acceptance criteria in the ticket:
```markdown
## Acceptance Criteria

- [ ] <criterion 1>  ← Must be ✅
- [ ] <criterion 2>  ← Must be ✅
- [ ] <criterion 3>  ← Must be ✅
```

### 5. Run Tests and Linting

```bash
# From project root:
./gradlew testDebugUnitTest    # Run unit tests
./gradlew lintKotlin           # Lint code (required)
./gradlew assembleDebug         # Verify build succeeds
```

### 6. Create Implementation Commit

Before requesting review:
```bash
git add <files>
git commit -m "ARCH-NNN: <ticket title>

<brief description of what changed>

Ticket: todo/ARCH-NNN.md
"
```

### 7. Request Review

Update the ticket:
```markdown
- **Status**: `In Progress`
- **Reviewer**: `<name of reviewer>`
```

Create a PR or ask a colleague to review the code. The reviewer should:
- Verify all acceptance criteria are met
- Check that `./gradlew lintKotlin` passes
- Validate that existing code is not broken
- Approve or request changes

### 8. Address Review Feedback

If changes requested:
- Make the changes
- Create a new commit (don't force-push, keep history clear)
- Update the ticket if acceptance criteria changed

### 9. Mark Done

Once review is approved and all commits are pushed:
```markdown
- **Status**: `Done`
- **Reviewer**: `<name of reviewer>`
```

Update `todo/INDEX.md` to reflect done status (move ticket from backlog to done section).

### 10. Log Improvements

In your final commit, add any process or code improvements to `docs/process-improvement-log.md`:
```markdown
## <Date> - ARCH-NNN

**What went well**:
- Item 1
- Item 2

**What could improve**:
- Item 1
- Item 2
```

## Phase 0 Walkthrough

To illustrate, here's how Phase 0 would progress:

### ARCH-030: Inventory (Week 1)

1. Read `todo/ARCH-030.md`
2. Mark `Status: In Progress`
3. Trace launch paths:
   - Find `MainActivity` entry point
   - Follow LaunchCoordinator
   - Trace container setup
   - Identify imagefs mutations
   - Document all in `docs/runtime-launch-flow-current-state.md`
4. Have a peer review the doc for accuracy
5. Commit: `ARCH-030: Inventory current runtime/container/imagefs launch flow`
6. Mark `Status: Done`

### ARCH-031: Fingerprinting (Week 1)

1. Read `todo/ARCH-031.md` (depends on ARCH-030)
2. Mark `Status: In Progress`
3. Create data class:
   ```kotlin
   // app/src/main/java/app/gamegrub/telemetry/session/LaunchFingerprint.kt
   data class LaunchFingerprint(
       val sessionId: String,  // UUID
       val timestamp: Long,
       val baseId: String?,
       val runtimeId: String?,
       val driverId: String?,
       val containerId: String,
       val gameTitle: String,
       val gamePlatform: String,
       val deviceClass: String
   )
   ```
4. Add logging hooks in LaunchCoordinator
5. Create unit tests
6. Run `./gradlew testDebugUnitTest` → PASS
7. Run `./gradlew lintKotlin` → PASS
8. Commit and request review
9. Address feedback
10. Mark `Status: Done`

### ARCH-032: Failure Taxonomy (Week 1.5)

1. Read `todo/ARCH-032.md` (depends on ARCH-030)
2. Mark `Status: In Progress`
3. Create enum and doc
4. Create unit tests
5. Verify, commit, review, mark done

### ARCH-033: Milestones (Week 2)

1. Read `todo/ARCH-033.md` (depends on ARCH-031)
2. Mark `Status: In Progress`
3. Create `LaunchMilestone` enum and `MilestoneRecorder` service
4. Wire into launch paths
5. Tests, commit, review, done

### End of Phase 0

- All 4 tickets done
- Launches record fingerprints and milestones
- Failure taxonomy documented
- Zero breaking changes
- Ready to start Phase 1

## Dealing with Blockers

### If You Get Stuck

1. **Check dependencies**: Is a prerequisite ticket really done?
2. **Read AGENTS.md**: Coding standards, project structure
3. **Ask for help**: Don't silently expand scope; create a child ticket

### If You Discover New Work

**Don't add it to the current ticket.** Create a new child ticket:

```markdown
# ARCH-NNN-X - New discovered work

- **ID**: `ARCH-NNN-X`
- **Area**: `same as parent`
- **Status**: `Backlog`
- **Parent**: `ARCH-NNN`
- ...
```

Link it in your current ticket's `Dependencies` section and in `todo/INDEX.md`.

### If a Ticket Is Too Large

If you realize a ticket can't be done in 1 week, **stop and decompose it**:

1. Mark the ticket `Status: Blocked`
2. Add to `Blocker` section why it's blocked
3. Create child tickets for smaller pieces
4. Update `Dependencies` to reference children
5. Update `todo/INDEX.md` to link children

## Phase Transition Rules

### Phase 0 → Phase 1

**ONLY after all 4 Phase 0 tickets are `Done`**:
1. Verify telemetry and fingerprinting are working
2. Update `todo/INDEX.md` to mark Phase 0 complete
3. Create tickets for Phase 1 (they already exist: ARCH-034-037)
4. Start ARCH-034

### Phase 1 → Phase 2

**ONLY after all 4 Phase 1 tickets are `Done`**:
1. Verify manifests serialize/deserialize correctly
2. Write a brief document: `docs/phase-1-completion-notes.md`
3. Start Phase 2 (tickets already exist: ARCH-038-041)

## Documentation Updates Required

For each ticket, update any relevant docs:

- **Phase 0 docs**:
  - `docs/runtime-launch-flow-current-state.md` (ARCH-030)
  - `docs/launch-failure-taxonomy.md` (ARCH-032)

- **Phase 2 docs**:
  - `docs/runtime-store-schema.md` (ARCH-038)
  - `docs/container-store-schema.md` (ARCH-040)

- **Phase 3 docs**:
  - `docs/cache-controller-design.md` (ARCH-042)

All docs are listed in ticket acceptance criteria.

## Quick Reference: Ticket Commands

```bash
# Check build
./gradlew assembleDebug

# Run all unit tests
./gradlew testDebugUnitTest

# Run single test class
./gradlew testDebugUnitTest --tests "app.gamegrub.telemetry.session.LaunchFingerprintTest"

# Run single test method
./gradlew testDebugUnitTest --tests "app.gamegrub.telemetry.session.LaunchFingerprintTest.serialize_roundtrip"

# Lint (required for all PRs)
./gradlew lintKotlin formatKotlin

# View lint errors
./gradlew lintKotlin
```

## Getting Help

- **Coding questions**: See `AGENTS.md` for guidelines
- **Architecture questions**: See `docs/ARCHITECTURE.md`
- **Build issues**: See `AGENTS.md` build commands
- **Ticket scope questions**: Read the ticket and its dependencies
- **Blocking issues**: Document in ticket, create follow-up child tickets

## Related Files

- `todo/INDEX.md` - Master backlog (update when tickets change status)
- `todo/PHASES_0-3_OVERVIEW.md` - Phase 0-3 summary and dependencies
- `todo/PHASES_1-12_ROADMAP.md` - Full 12-phase vision
- `AGENTS.md` - Project structure and coding standards
- `docs/process-improvement-log.md` - Log improvements as you go

## Success Metrics for Phase 0-3

By the end of Phase 3, you should have:

✅ Launches recording fingerprints with base/runtime/driver/container IDs
✅ Manifest data classes for all 6 types (base, runtime, driver, profile, container, cache)
✅ RuntimeStore service managing immutable bundles
✅ ContainerStore service managing mutable containers
✅ CacheController service with deterministic key derivation
✅ All code passing linting and tests
✅ Zero breaking changes to existing launch behavior
✅ Documentation for all schemas and services

Ready to proceed? Start with `todo/ARCH-030.md`!

