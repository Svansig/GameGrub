# ARCH-053 - Wire SessionPlan consumption in launch orchestrator

- **ID**: `ARCH-053`
- **Area**: `launch engine`
- **Priority**: `P1`
- **Status**: `Reopened`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Integration only.`

## Overview

This ticket wires SessionPlan consumption into the existing launch orchestrator.

## Implementation

The integration is done by importing LaunchEngine into GameLaunchOrchestrator and calling session assembly before launch. The fingerprint and milestone emitters are already wired.

## Acceptance Criteria

- [x] SessionPlan assembly before launch
- [x] LaunchEngine integration with orchestrator
- [x] Session state transitions tracked

## Related Files

- `app/src/main/java/app/gamegrub/launch/LaunchEngine.kt`
- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt`
