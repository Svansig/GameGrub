# ARCH-066 - Define storage policy model

- **ID**: `ARCH-066`
- **Area**: `storage + policy`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket defines the storage policy model for internal vs external storage.

## Implementation

Created `StoragePolicy.kt` with:

- `StoragePolicy`: Sealed class for storage location policies
- `StorageLocation`: Enum for INTERNAL/EXTERNAL
- `StorageRequirement`: Data class for storage constraints

**Location**: `app/src/main/java/app/gamegrub/storage/StoragePolicy.kt`

## Acceptance Criteria

- [x] StoragePolicy sealed class
- [x] StorageLocation enum
- [x] StorageRequirement data class

## Related Files

- `app/src/main/java/app/gamegrub/storage/StoragePolicy.kt`