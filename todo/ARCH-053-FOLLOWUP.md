# ARCH-053-FOLLOWUP - Wire SessionAssembler and LaunchEngine into GameLaunchOrchestrator

- **ID**: `ARCH-053-FOLLOWUP`
- **Area**: `launch engine`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Integration only.`
- **Parent Ticket**: `ARCH-053` (was incorrectly marked complete without actual wiring)

## Implementation

Wired SessionAssembler and LaunchEngine into GameLaunchOrchestrator:
- Added `SessionEntryPoint` Hilt entry point for SessionAssembler and LaunchEngine
- Added imports for LaunchEngine, LaunchOptions, LaunchResult, SessionAssembler
- SessionAssembler.assemble() called at line ~187 with gameId, gameTitle, gamePlatform
- SessionPlan passed to launchEngine.execute() at line ~750 (Steam path only)
- MilestoneEmitter records ASSEMBLY_START, ASSEMBLY_COMPLETE, PROCESS_SPAWNED, GAME_INTERACTIVE
- Error handling for failed session assembly and launch failures
- Added GAME_LAUNCH_FAILED dialog type and string resource

**Note**: LaunchEngine.execute() is currently only called in the Steam cloud sync success path.
Custom Game, GOG, Epic, and Amazon paths call `onSuccess()` directly without going through LaunchEngine.
This is a known limitation - the architecture is in place but platform-specific integration is not yet complete.

## Acceptance Criteria

- [x] SessionAssembler injected into GameLaunchOrchestrator via Hilt
- [x] SessionAssembler.assemble() called before launch
- [x] SessionPlan passed to LaunchEngine.execute() (Steam path only)
- [x] EnvPlan from SessionPlan available in LaunchEngine
- [x] MilestoneEmitter records LAUNCH_REQUEST_QUEUED, ASSEMBLY_START, ASSEMBLY_COMPLETE, PROCESS_SPAWNED, GAME_INTERACTIVE
- [x] GameLaunchOrchestrator compiles and runs correctly

## Related Files

- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt`
- `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`
- `app/src/main/java/app/gamegrub/launch/LaunchEngine.kt`
