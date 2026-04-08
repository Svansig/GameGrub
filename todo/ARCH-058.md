# ARCH-058 - Add read/query APIs for launch records

- **ID**: `ARCH-058`
- **Area**: `telemetry + records`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - API implementation only.`

## Overview

This ticket adds read/query APIs for launch records.

## Implementation

The LaunchRecordStore (ARCH-057) already provides comprehensive query APIs:

- `getRecord(sessionId)`: Get single record
- `getRecordsByTitle(titleId)`: Query by game title
- `getRecentRecords(limit)`: Get recent records
- `getSuccessfulRecords()`: Filter success only
- `getFailedRecords()`: Filter failures only
- `getLastKnownGood(titleId)`: Get most recent successful launch
- `deleteRecord(sessionId)`: Delete a record
- `clearOldRecords(maxAgeMs)`: GC old records

## Acceptance Criteria

- [x] Query by session ID
- [x] Query by title ID
- [x] Query recent records
- [x] Filter by outcome
- [x] Get last known good

## Related Files

- `app/src/main/java/app/gamegrub/telemetry/record/LaunchRecordStore.kt`