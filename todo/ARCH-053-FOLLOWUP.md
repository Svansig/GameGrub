# ARCH-053-FOLLOWUP - Wire SessionAssembler and LaunchEngine into GameLaunchOrchestrator

- **ID**: `ARCH-053-FOLLOWUP`
- **Area**: `launch engine`
- **Priority**: `P1`
- **Status**: `Reopened`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Integration only.`
- **Parent Ticket**: `ARCH-053` (was incorrectly marked complete without actual wiring)

## Implementation

Wired SessionAssembler and LaunchEngine into GameLaunchOrchestrator:
- Added `SessionEntryPoint` Hilt entry point for SessionAssembler and LaunchEngine
- Added imports for LaunchEngine, LaunchOptions, LaunchResult, SessionAssembler
- SessionAssembler.assemble() called at line ~187 with gameId, gameTitle, gamePlatform
- SessionPlan passed to launchEngine.execute() at line ~750
- MilestoneEmitter records ASSEMBLY_START, ASSEMBLY_COMPLETE, PROCESS_SPAWNED, GAME_INTERACTIVE
- Error handling for failed session assembly and launch failures

## Acceptance Criteria

- [x] SessionAssembler injected into GameLaunchOrchestrator via Hilt
- [x] SessionAssembler.assemble() called before launch
- [ ] SessionPlan passed to LaunchEngine.execute() — **PARTIAL: `launchEngine.execute()` called only in the `SyncResult.UpToDate`/`SyncResult.Success` Steam branch (line 742). GOG, Epic, Custom Game, and Amazon platforms all bypass `launchEngine.execute()` and call `onSuccess()` directly.**
- [ ] EnvPlan from SessionPlan flows into launch command builder (deferred to ARCH-054-FOLLOWUP)
- [x] MilestoneEmitter records LAUNCH_REQUEST_QUEUED, ASSEMBLY_START, ASSEMBLY_COMPLETE, PROCESS_SPAWNED, GAME_INTERACTIVE
- [x] GameLaunchOrchestrator compiles and runs correctly

## Related Files

- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt`
- `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`
- `app/src/main/java/app/gamegrub/launch/LaunchEngine.kt`
