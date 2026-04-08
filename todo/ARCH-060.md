# ARCH-060 - Define recommendation result model

- **ID**: `ARCH-060`
- **Area**: `recommendations`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Infrastructure only.`

## Overview

This ticket defines the recommendation result model for profile recommendations.

## Implementation

Created `RecommendationResult.kt` with:

- `RecommendationResult`: Sealed class for recommendation outcomes
- `Recommendation`: Data class for individual recommendations
- `RecommendationSource`: Enum for LOCAL/CURATED/FALLBACK

**Location**: `app/src/main/java/app/gamegrub/telemetry/recommendation/RecommendationResult.kt`

## Acceptance Criteria

- [x] RecommendationResult sealed class
- [x] Recommendation data class with confidence
- [x] Source enum for origin

## Related Files

- `app/src/main/java/app/gamegrub/telemetry/recommendation/RecommendationResult.kt`