# ARCH-050 - Implement Mesa shader cache adapter

- **ID**: `ARCH-050`
- **Area**: `graphics cache`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Implementation in ARCH-048.`

## Overview

This ticket implements the Mesa shader cache adapter.

## Implementation

The MesaCacheAdapter was implemented in GraphicsCacheAdapter.kt (ARCH-048):

- `MesaCacheAdapter`: Handles Mesa shader cache with MESA_SHADER_CACHE_DIR and XDG_CACHE_HOME
- Enables RADV_PERFTEST=rt for ray tracing optimization on supported GPUs

## Acceptance Criteria

- [x] Mesa shader cache directory creation
- [x] XDG_CACHE_HOME wiring
- [x] RADV perftest enablement
- [x] Linux-only support detection

## Related Files

- `app/src/main/java/app/gamegrub/graphics/cache/GraphicsCacheAdapter.kt`