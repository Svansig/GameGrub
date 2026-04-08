# ARCH-062 - Add curated rules scaffolding

- **ID**: `ARCH-062`
- **Area**: `recommendations`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket adds curated rules scaffolding for game-specific recommendations.

## Implementation

Created `CuratedRulesResolver.kt` that provides game-specific configuration rules:

- Game-specific runtime recommendations
- Driver compatibility rules
- Profile-specific overrides

**Location**: `app/src/main/java/app/gamegrub/telemetry/recommendation/CuratedRulesResolver.kt`

## Acceptance Criteria

- [x] Curated rules for known games
- [x] Default fallback rules
- [x] Rule lookup API

## Related Files

- `app/src/main/java/app/gamegrub/telemetry/recommendation/CuratedRulesResolver.kt`