# ARCH-044 - Define SessionPlan model for launch-time composition

- **ID**: `ARCH-044`
- **Area**: `session assembler`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket defines the SessionPlan model that represents the complete launch-time composition of base, runtime, driver, container, caches, and metadata.

## Implementation

Created `SessionPlan.kt` with:

- `SessionPlan`: Data class capturing all composition components
- `SessionMetadata`: Session identification and timing
- `SessionState`: Enum for session lifecycle (COMPOSING, ASSEMBLED, LAUNCHED, COMPLETED, FAILED)
- `SessionComposition`: Sealed class for composition result

**Location**: `app/src/main/java/app/gamegrub/session/model/SessionPlan.kt`

## Acceptance Criteria

- [x] SessionPlan data class with all composition components
- [x] SessionMetadata with sessionId, timestamps, game info
- [x] SessionState enum for lifecycle tracking
- [x] SessionComposition sealed class for result handling
- [x] Serialization support via kotlinx-serialization

## Related Files

- `app/src/main/java/app/gamegrub/session/model/SessionPlan.kt`
- `app/src/test/java/app/gamegrub/session/model/SessionPlanTest.kt`