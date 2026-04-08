# ARCH-030 - Inventory current runtime/container/imagefs launch flow

- **ID**: `ARCH-030`
- **Area**: `launch + container + runtime`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/runtime-launch-flow-current-state.md documenting existing paths, mutation points, and driver extraction flow.`
- **Reviewer**: `TBD`

## Problem

The current launch path mixes shared mutable imagefs state, per-container prefix state, driver extraction, and runtime mutation in ways that are difficult to trace. Understanding what currently happens is a prerequisite for all subsequent refactoring.

## Scope

- In scope:
  - Map the current launch flow from app entry point through container startup
  - Identify all mutation points on shared runtime/imagefs state
  - Identify all driver extraction and path substitution points
  - Identify all container-specific state initialization
  - Document the current failure modes and recovery paths
  - Create a structured audit document with file references
- Out of scope:
  - Code changes or refactoring (purely investigative)
  - Proposing new architecture (covered in later phases)

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `ARCH-031`, `ARCH-032`, `ARCH-033`
- Related follow-ups: `ARCH-040`, `ARCH-041` (later phases)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Document created at `docs/runtime-launch-flow-current-state.md` with:
  - [ ] Entry points (MainActivity, deep links, external intents, LAUNCH_GAME action)
  - [ ] Launch orchestration path through LaunchCoordinator and UILaunchOrchestrator
  - [ ] Container setup and prefix initialization paths
  - [ ] Shared imagefs/runtime extraction and mutation points
  - [ ] Driver extraction and substitution points
  - [ ] Current failure recovery and re-extraction logic
  - [ ] File references and line numbers for each step
- [ ] A reference map linking key files to their role in current launch
- [ ] Current assumptions about shared state mutability documented
- [ ] No code changes (purely documentation)

## Validation

- [ ] Document is legible and cross-referenced accurately
- [ ] All major entry points are represented
- [ ] A second reviewer validates the flow against actual code
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/runtime-launch-flow-current-state.md` (to be created)
- Related PR: `TBD`
- Related commit(s): `TBD`

