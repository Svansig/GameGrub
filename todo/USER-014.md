# USER-014 - Launch Installed Game

- **ID**: `USER-014`
- **Area**: `launch`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to launch an installed game so that I can play it on my device.

## Problem

The core functionality - users need to actually play their games.

## Scope

- In scope:
  - Launch button on game detail screen (for installed games)
  - Container/server startup (XServer/Wine)
  - Game launch command execution
  - Pre-launch checks (storage, compatibility)
  - Launch success confirmation
  - Clear error messages on launch failure

## Acceptance Criteria

- [ ] User can launch installed game with single tap
- [ ] XServer/container starts automatically if needed
- [ ] Game launches and runs without immediate crashes
- [ ] Clear error shown if launch fails
- [ ] Launch works without any manual configuration for supported games

## Validation

- [ ] Manual flow: Launch installed game
- [ ] Manual flow: Verify game runs
- [ ] Manual flow: Handle launch failure gracefully