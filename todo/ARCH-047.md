# ARCH-047 - Add session artifact serialization (mounts.json, env.json, launch.json)

- **ID**: `ARCH-047`
- **Area**: `session assembler`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required - Service implementation only.`

## Overview

This ticket adds session artifact serialization for debugging and replay.

## Implementation

Created `SessionArtifactWriter.kt` that serializes SessionPlan to:

- `mounts.json`: Mount plan with all mount points
- `env.json`: Environment variables and path additions
- `launch.json`: Session metadata and composition

**Location**: `app/src/main/java/app/gamegrub/session/SessionArtifactWriter.kt`

## Acceptance Criteria

- [x] Serializes SessionPlan to mounts.json
- [x] Serializes SessionPlan to env.json
- [x] Serializes SessionPlan to launch.json
- [x] Includes all relevant metadata
- [x] Pretty-printed JSON output

## Related Files

- `app/src/main/java/app/gamegrub/session/SessionArtifactWriter.kt`