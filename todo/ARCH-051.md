# ARCH-051 - Wire XDG cache and pre-launch directory creation

- **ID**: `ARCH-051`
- **Area**: `graphics cache`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Implementation in ARCH-048.`

## Overview

This ticket wires XDG cache paths and pre-launch directory creation into the launch flow.

## Implementation

The wiring is handled via GraphicsCacheAdapter's setup() method which:
- Creates cache directories before launch
- Wires XDG_CACHE_HOME environment variable
- Integrates with SessionAssembler's cache handle resolution

## Acceptance Criteria

- [x] Cache directories created before launch
- [x] XDG_CACHE_HOME injected into environment
- [x] Pre-launch validation of cache paths
- [x] Integration with SessionAssembler

## Related Files

- `app/src/main/java/app/gamegrub/graphics/cache/GraphicsCacheAdapter.kt`
- `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`