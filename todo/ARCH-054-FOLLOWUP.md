# ARCH-054-FOLLOWUP - Integrate EnvPlan from SessionPlan into actual launch command

- **ID**: `ARCH-054-FOLLOWUP`
- **Area**: `launch engine`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Integration only.`
- **Parent Ticket**: `ARCH-054` (was incorrectly marked complete without actual integration)

## Implementation

EnvPlan integration is now partially complete via ARCH-053-FOLLOWUP:
- SessionPlan.envPlan is available to LaunchEngine.execute()
- LaunchEngine receives the SessionPlan and can access envPlan
- Full env var injection into container launch requires additional work in LaunchEngine.executeContainerLaunch()

## Implementation

Env var injection from `EnvPlan` into the container process is now wired via a bridge pattern:

- Created `ActiveSessionStore` (`launch/ActiveSessionStore.kt`) — a process-lifetime singleton holding the current `SessionPlan` between assembly and XEnvironment setup
- `LaunchEngine.execute()` calls `ActiveSessionStore.setActiveSession(updatedPlan)` after assembly completes
- `EnvironmentSetupCoordinator.setupXEnvironment()` calls `applySessionEnvPlan(envVars)` after `envVars.putAll(nonNullContainer.envVars)`, applying non-conflicting env vars from the session plan's `EnvPlan.environmentVariables` into the real `EnvVars` object consumed by the guest launcher
- `ActiveSessionStore.clearActiveSession()` called after `environment.startEnvironmentComponents()` (and on failure cleanup)

### What is applied
Cache paths and driver hints that are net-new to the existing setup: `DXVK_STATE_CACHE`, `MESA_SHADER_CACHE_DIR`, `XDG_CACHE_HOME`, `RADV_PERFTEST`, `GG_BASE_VERSION`, and any future keys added to `EnvPlan`.

### Migration denylist
Keys managed by existing legacy logic are excluded and will be migrated incrementally: `WINEPREFIX` (imageFs path), `WINEDEBUG` (PrefManager), `LC_ALL` (container field), `MESA_DEBUG`, `MESA_NO_ERROR` (hardcoded).

## Acceptance Criteria

- [x] EnvPlan cache paths applied to real `EnvVars` object used by GuestProgramLauncherComponent
- [x] WINEPREFIX preserved from legacy imageFs path during migration
- [x] ActiveSessionStore cleared after environment starts (no stale state across launches)
- [x] Build compiles clean

## Related Files

- `app/src/main/java/app/gamegrub/launch/ActiveSessionStore.kt`
- `app/src/main/java/app/gamegrub/launch/LaunchEngine.kt`
- `app/src/main/java/app/gamegrub/container/launch/env/EnvironmentSetupCoordinator.kt`
