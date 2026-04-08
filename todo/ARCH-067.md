# ARCH-067 - Implement split-root container support

- **ID**: `ARCH-067`
- **Area**: `storage + containers`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Implementation in ContainerStore.`

## Overview

This ticket implements split-root container support.

## Implementation

The ContainerStore (Phase 2) already supports separate directories for:
- Prefix (internal)
- Cache (internal)
- Install (configurable internal/external)
- Saves (internal)

This can be extended via StoragePolicy integration.

## Acceptance Criteria

- [x] Separate prefix/cache/install paths
- [x] Storage location configuration
- [x] Volume availability checks

## Related Files

- `app/src/main/java/app/gamegrub/container/store/ContainerStoreSchema.kt`
- `app/src/main/java/app/gamegrub/storage/StoragePolicy.kt`