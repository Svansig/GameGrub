# ARCH-048 - Define graphics cache adapter abstraction

- **ID**: `ARCH-048`
- **Area**: `graphics cache`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket defines the abstraction for graphics cache adapters that can inject cache paths into Wine/Proton environments.

## Implementation

Created `GraphicsCacheAdapter.kt` with:

- `GraphicsCacheAdapter`: Interface for cache-aware components
- `CacheCapability`: Enum for adapter capabilities
- `CacheConfiguration`: Data class for cache path configuration

**Location**: `app/src/main/java/app/gamegrub/graphics/cache/GraphicsCacheAdapter.kt`

## Acceptance Criteria

- [x] GraphicsCacheAdapter interface with setup/configure/cleanup
- [x] CacheCapability enum for DXVK/VKD3D/Mesa/Generic
- [x] CacheConfiguration data class for path injection
- [x] Fallback handling for unsupported adapters

## Related Files

- `app/src/main/java/app/gamegrub/graphics/cache/GraphicsCacheAdapter.kt`