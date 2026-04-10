# Runtime Exit Contract

This document defines what "clean exit" means for a game session and how to validate it.

## Scope

Applies to session teardown initiated through `XServerExitCoordinator.requestExit(...)`.

## Clean Exit Contract

A teardown is considered clean when all of the following are true:

1. Exit orchestration runs once per session (duplicate triggers are ignored).
2. Runtime teardown phases complete without unhandled exceptions.
3. Environment components stop in reverse startup order.
4. No environment component failure is allowed to abort teardown of remaining components.
5. Session process teardown is initiated (`GuestProgramLauncherComponent.stop()` + `wineserver -k`).
6. Runtime references are cleared (`XEnvironment`, input controls, touchpad, suspend state).
7. Session summary write is attempted after teardown.
8. Post-exit health checks run for lingering runtime threads and wine processes.
9. A structured teardown report is emitted with durations and failure status.

## Teardown Phases

`XServerExitCoordinator` emits a structured phase report with these phases:

- `StopSessionWatchers`
- `StopInputAndWinHandler`
- `StopEnvironmentComponents`
- `ClearRuntimeReferences`
- `WriteSessionSummary`
- `PostExitHealthChecks`

## Environment Stop Requirements

`XEnvironment.stopEnvironmentComponentsWithSummary()` must:

- stop components in reverse order,
- capture per-component duration,
- capture per-component success/failure,
- continue stopping remaining components even after failures,
- return `EnvironmentStopSummary` for higher-level reporting.

## Validation Checklist (Manual)

For a stop action in a running game session, verify:

- no repeated teardown loops are started,
- teardown report appears exactly once,
- `envFailed=0` for healthy runs,
- `leftoverWine=0` and `leftoverThreads=0` for healthy runs,
- no "timed out waiting" warnings for connector/handler threads,
- app navigates back to home without hang.

## Follow-Up Automation Targets

- Add a teardown smoke test that asserts phase order and no phase failure.
- Add optional process/thread post-check probes where available.

