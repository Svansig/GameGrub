# ARCH-067 - Implement split-root container support

- **ID**: `ARCH-067`
- **Area**: `storage + containers`
- **Priority**: `P1`
- **Status**: `Reopened`
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

- [x] Separate prefix/cache/install paths — `ContainerStoreSchema` provides distinct `prefixDir`, `cacheDir`, `installDir`, `savesDir`
- [x] Storage location configuration — `StoragePolicy` sealed class with `HotRuntime`/`ColdBulk`/`Hybrid` variants
- [ ] Volume availability checks — **FAILED: `StoragePolicyHelper.isLocationAvailable()` is a stub that hardcodes `return location == StorageLocation.INTERNAL`. No Android storage volume APIs (`Environment.getExternalStorageState()`, `StorageManager`) are consulted. External/SD support is non-functional.**

## Related Files

- `app/src/main/java/app/gamegrub/container/store/ContainerStoreSchema.kt`
- `app/src/main/java/app/gamegrub/storage/StoragePolicy.kt`