# USER-084 - Configure Vulkan/GLES

- **ID**: `USER-084`
- **Area**: `settings/graphics`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to configure which graphics backend is used so that I can optimize for compatibility or performance.

## Problem

Different games may work better with different graphics APIs.

## Scope

- In scope:
  - Select default graphics backend (Vulkan, OpenGL, GLES)
  - Per-game override option
  - Automatic selection option

## Acceptance Criteria

- [ ] User can select graphics backend
- [ ] User can override per-game
- [ ] Setting persists

## Validation

- [ ] Manual flow: Change graphics backend