# ARCH-046 - Implement SessionAssembler service for launch-time composition

- **ID**: `ARCH-046`
- **Area**: `session assembler`
- **Priority**: `P1`
- **Status**: `Reopened`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`

## Overview

This ticket implements the SessionAssembler service that composes all components at launch time.

## Implementation

Created `SessionAssembler.kt` with:

- `SessionAssembler`: Service for composing SessionPlan from manifests/stores
- `assemble()`: Main composition method
- `resolveEnvironmentVariables()`: Resolves env vars based on composition
- `resolveMountPaths()`: Resolves mount paths based on stores

**Location**: `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`

## Acceptance Criteria

- [x] SessionAssembler composes SessionPlan from stores
- [x] Resolves environment variables based on composition
- [x] Resolves mount paths for all components
- [ ] Handles cache handles for shader/translator/probe caches — **BUG: `resolveCacheHandles()` creates directories but never appends any `CacheHandle` to its list. Always returns `emptyList()`. `CacheHandle.fromManifest()` is never called. `sessionPlan.cacheHandles` is always empty.**
- [x] Hilt-injectable @Singleton

## Related Files

- `app/src/main/java/app/gamegrub/session/SessionAssembler.kt`
- `app/src/main/java/app/gamegrub/session/SessionAssemblerModule.kt`