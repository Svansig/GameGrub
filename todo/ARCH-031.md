# ARCH-031 - Add structured launch fingerprinting and telemetry hooks

- **ID**: `ARCH-031`
- **Area**: `launch + telemetry`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only; logging conventions documented in existing telemetry guidelines.`
- **Reviewer**: `TBD`

## Problem

Launches today lack structured visibility into what runtime, driver, profile, and container versions are in use. Telemetry is ad-hoc and difficult to correlate with success/failure. This makes it impossible to reason about which configuration combinations work.

## Scope

- In scope:
  - Define a `LaunchFingerprint` data class capturing:
    - Session ID (unique per launch)
    - Timestamp
    - Base bundle ID/version (if identified)
    - Runtime ID/version (Wine/Proton/compatibility layer version)
    - Driver bundle ID/version (graphics userspace)
    - Container ID/path
    - Game title and platform (Steam/GOG/Epic/Amazon)
    - Device class (from DeviceQueryGateway)
  - Add logging hooks at key milestones (assembler, launcher, process start, backend init)
  - Implement structured JSON emission for fingerprints (for local analysis)
  - Wire fingerprinting into existing LaunchCoordinator/UILaunchOrchestrator paths
  - Tests for fingerprint construction and emission
- Out of scope:
  - Remote telemetry (local-only analysis)
  - Changing launch behavior

## Dependencies and Decomposition

- Parent ticket: `ARCH-030`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-032` (failure taxonomy), `ARCH-035` (metrics per-milestone)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] `LaunchFingerprint` data class created in `app.gamegrub.telemetry.session`
- [ ] Fingerprint JSON serialization works (using kotlinx-serialization or Moshi)
- [ ] Logging hooks added at:
  - [ ] Launch request received
  - [ ] Assembly starts
  - [ ] Assembly complete with resolved bundle IDs
  - [ ] Process spawned
  - [ ] Backend initialized (first render or comparable milestone)
- [ ] Fingerprints are written to local app-private directory for analysis
- [ ] Unit tests verify fingerprint construction and JSON roundtrip
- [ ] Existing launch code still works (no behavior changes)

## Validation

- [ ] Manual launch of a Steam/GOG/Epic/Amazon game produces a fingerprint file
- [ ] Fingerprints can be read and parsed locally
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

