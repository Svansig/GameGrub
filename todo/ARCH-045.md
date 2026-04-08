# ARCH-045 - Define mount/path mapping and env-var models for session assembly

- **ID**: `ARCH-045`
- **Area**: `session assembler`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Already covered in SessionPlan models.`

## Overview

This ticket defines the mount/path mapping and environment variable models for session assembly.

## Implementation

The MountPlan and EnvPlan models were already included in SessionPlan.kt (ARCH-044):

- `MountPlan`: Data class with mount paths for base, runtime, driver, containers, caches
- `BindMount`: Data class for additional bind mounts
- `EnvPlan`: Data class with environment variables, path additions, and cache paths

The models support:
- DXVK state cache path injection
- Mesa shader cache path injection
- XDG_CACHE_HOME configuration
- Wine runtime and prefix configuration
- Custom path additions

## Acceptance Criteria

- [x] MountPlan with all required mount paths
- [x] EnvPlan with environment variable support
- [x] Cache path injection for DXVK/Mesa
- [x] XDG_CACHE_HOME support
- [x] Serialization support

## Related Files

- `app/src/main/java/app/gamegrub/session/model/SessionPlan.kt`