# ARCH-049 - Implement DXVK and VKD3D cache adapters

- **ID**: `ARCH-049`
- **Area**: `graphics cache`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Implementation in ARCH-048.`

## Overview

This ticket implements DXVK and VKD3D cache adapters.

## Implementation

The DxvkCacheAdapter and Vkd3dCacheAdapter were implemented in GraphicsCacheAdapter.kt (ARCH-048):

- `DxvkCacheAdapter`: Handles DXVK state cache with DXVK_ASYNC and DXVK_STATE_CACHE env vars
- `Vkd3dCacheAdapter`: Handles VKD3D shader cache with VKD3D_SHADER_CACHE and VKD3D_STATE_CACHE env vars

Both support:
- Directory creation
- Environment variable injection
- Cleanup on session end

## Acceptance Criteria

- [x] DXVK state cache path injection
- [x] VKD3D shader cache path injection
- [x] Async compute enablement
- [x] Directory creation and cleanup

## Related Files

- `app/src/main/java/app/gamegrub/graphics/cache/GraphicsCacheAdapter.kt`