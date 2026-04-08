# ARCH-032 - Define launch failure taxonomy and recovery phases

- **ID**: `ARCH-032`
- **Area**: `launch + error handling`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/launch-failure-taxonomy.md classifying failure modes, recovery behaviors, and detection strategies.`
- **Reviewer**: `TBD`

## Problem

Launches fail in many ways (process spawn failures, backend crashes, container setup errors, missing drivers, corrupted cache, etc.), but there is no consistent taxonomy for understanding what class of failure happened and what safe recovery actions exist.

## Scope

- In scope:
  - Define a `FailureClass` enum covering at least:
    - PROCESS_SPAWN (unable to start process)
    - BACKEND_INIT (wine/proton startup failure)
    - GRAPHICS_INIT (driver or rendering init failure)
    - CONTAINER_SETUP (prefix/home/mount setup error)
    - MISSING_DRIVER (graphics driver not found/installed)
    - CORRUPTED_CACHE (shader cache or translated code corrupted)
    - TIMEOUT (launch exceeded time limit)
    - UNKNOWN (unclassified)
  - Document detection strategy for each class (process exit code, stderr patterns, timing)
  - Map each failure class to safe recovery actions (none, cache invalidation, re-extract, fallback profile, etc.)
  - Create structured `LaunchFailureRecord` for storing outcomes
  - Document these in a reference doc
- Out of scope:
  - Implementing recovery logic (covered in later phases)
  - Changing existing error handling

## Dependencies and Decomposition

- Parent ticket: `ARCH-030`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-035` (structured outcome records), `ARCH-042` (adaptive fallback in Phase 10)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `FailureClass` enum defined in `app.gamegrub.launch.error`
- [ ] `LaunchFailureRecord` data class defined with:
  - [ ] Failure class
  - [ ] Detected root cause (if identifiable)
  - [ ] Stderr/stdout snippets (first N lines)
  - [ ] Process exit code
  - [ ] Time to failure
  - [ ] Recovery action taken (if any)
- [ ] Documentation created at `docs/launch-failure-taxonomy.md` covering:
  - [ ] Each failure class, detection strategy, and safe recovery actions
  - [ ] Process exit code conventions
  - [ ] Stderr pattern matching rules
  - [ ] Recovery action precedence
- [ ] Unit tests for failure record construction
- [ ] No behavior changes to existing launch

## Validation

- [ ] Failure classes cover common observed launch failure modes
- [ ] A second reviewer validates against real observed failures
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/launch-failure-taxonomy.md` (to be created)
- Related PR: `TBD`
- Related commit(s): `TBD`

