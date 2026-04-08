# ARCH-068 - Identify remaining shared-runtime mutation points

- **ID**: `ARCH-068`
- **Area**: `migration`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/mutation-points-audit.md documenting remaining shared runtime mutations.`

## Overview

This ticket identifies remaining shared runtime mutation points that need migration.

## Implementation

Based on the initial flow documentation (ARCH-030), these areas have been identified:

1. **ImageFs singleton**: Single INSTANCE shared across containers
2. **Wine path mutation**: `imageFs.setWinePath()` called per launch
3. **Cache directories**: Shared `.cache`, `.config` not isolated
4. **Pattern extraction**: `extractPattern()` called each launch

## Acceptance Criteria

- [x] Mutation points documented in migration audit

## Related Files

- `docs/runtime-launch-flow-current-state.md`
- `docs/mutation-points-audit.md`