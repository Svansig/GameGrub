# ARCH-056 - Define LaunchSessionRecord schema for structured launch outcomes

- **ID**: `ARCH-056`
- **Area**: `telemetry + records`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket defines the LaunchSessionRecord schema for persisting structured launch outcomes.

## Implementation

Created `LaunchSessionRecord.kt` with:

- `LaunchSessionRecord`: Data class for launch outcomes with all required fields
- `LaunchOutcome`: Enum for SUCCESS/FAILURE
- `SessionMilestone`: Data class for milestone timeline

**Location**: `app/src/main/java/app/gamegrub/telemetry/record/LaunchSessionRecord.kt`

## Acceptance Criteria

- [x] LaunchSessionRecord with title, device class, bundle IDs, milestones, duration, exit type
- [x] LaunchOutcome enum
- [x] SessionMilestone data class
- [x] Serialization support

## Related Files

- `app/src/main/java/app/gamegrub/telemetry/record/LaunchSessionRecord.kt`