# ARCH-057 - Implement local persistence for launch records

- **ID**: `ARCH-057`
- **Area**: `telemetry + records`
- **Priority**: `P1`
- **Status**: `Reopened`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`

## Overview

This ticket implements local persistence for launch records.

## Implementation

Created `LaunchRecordStore.kt` with:

- `LaunchRecordStore`: Service for persisting launch records to disk
- JSON-based file storage in app-private directory

**Location**: `app/src/main/java/app/gamegrub/telemetry/record/LaunchRecordStore.kt`

## Acceptance Criteria

- [x] JSON file-based persistence — `saveRecord()` uses `Json.encodeToString` + file write; correct implementation
- [ ] Record creation and storage — **FAILED: `saveRecord()` is never called from any launch path. Zero call sites in the codebase. The API exists but is dead code. No records are ever persisted.**
- [x] Directory management — `recordsDir` lazy init with `.mkdirs()`
- [x] Hilt-injectable @Singleton

## Related Files

- `app/src/main/java/app/gamegrub/telemetry/record/LaunchRecordStore.kt`