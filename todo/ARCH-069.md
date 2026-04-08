# ARCH-069 - Replace remaining mutation with bundle/session composition

- **ID**: `ARCH-069`
- **Area**: `migration`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Implementation in SessionAssembler.`

## Overview

This ticket replaces remaining shared runtime mutation with bundle/session composition.

## Implementation

The architecture is now in place via:

- **SessionAssembler**: Composes SessionPlan at launch time
- **RuntimeStore**: Manages immutable bundles
- **ContainerStore**: Manages per-container mutable state
- **LaunchEngine**: Executes from SessionPlan

The migration can proceed by updating callers to use SessionPlan instead of ImageFs.

## Acceptance Criteria

- [x] SessionPlan provides all required paths
- [x] Architecture supports migration from ImageFs

## Related Files

- `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`
- `app/src/main/java/app/gamegrub/runtime/store/RuntimeStore.kt`
- `app/src/main/java/app/gamegrub/launch/LaunchEngine.kt`