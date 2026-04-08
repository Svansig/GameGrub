# ARCH-033 - Define milestones and structured outcome recording for launches

- **ID**: `ARCH-033`
- **Area**: `launch + telemetry`
- **Priority**: `P0`
- **Status**: `Reopened`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only; extends ARCH-031 fingerprinting.`
- **Reviewer**: `TBD`

## Problem

Launch telemetry today is scattered and opportunistic. We need structured milestone events (process start, backend init, first frame, etc.) to understand where time is spent and where failures occur.

## Scope

- In scope:
  - Define a `LaunchMilestone` enum:
    - LAUNCH_REQUEST_QUEUED
    - ASSEMBLY_START
    - ASSEMBLY_COMPLETE
    - BUNDLE_VERIFICATION_COMPLETE
    - CONTAINER_READY
    - PROCESS_SPAWNED
    - BACKEND_INITIALIZED
    - FIRST_FRAME_RENDERED
    - GAME_INTERACTIVE (if detectable)
    - LAUNCH_TIMEOUT
    - LAUNCH_FAILED
  - Create `MilestoneRecord` with timestamp and optional metadata
  - Add milestone recording hooks in LaunchCoordinator, LaunchEngine (to be created in Phase 7), container startup
  - Store milestone timeline in LaunchSessionRecord (created in Phase 8)
  - Unit tests for milestone recording
- Out of scope:
  - Analyzing or using milestone data (covered in later phases)
  - Changing existing launch paths

## Dependencies and Decomposition

- Parent ticket: `ARCH-031`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-035` (extended telemetry records), `ARCH-040` (session persistence)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `LaunchMilestone` enum created
- [ ] `MilestoneRecord` data class created with timestamp and metadata
- [ ] `MilestoneRecorder` service created to collect and query milestones
- [ ] Milestone hooks added to:
  - [ ] LaunchCoordinator (LAUNCH_REQUEST_QUEUED, ASSEMBLY_START)
  - [ ] Key entry points (LAUNCH_REQUEST_QUEUED if not already)
  - [ ] Container initialization (CONTAINER_READY)
  - [ ] Process spawn (PROCESS_SPAWNED)
- [ ] Unit tests for milestone recording and timeline queries
- [ ] Fingerprinting and milestone recording work together without conflicts

## Validation

- [ ] Manual launch records milestones (can be viewed in logs or test output)
- [ ] Milestone timestamps are monotonic
- [ ] `./gradlew testDebugUnitTest` passes
- [ ] `./gradlew lintKotlin` passes for touched files
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/runtime-launch-flow-current-state.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

