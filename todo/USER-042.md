# USER-042 - Exit Game Properly

- **ID**: `USER-042`
- **Area**: `gameplay`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to exit a running game properly so that my progress is saved and cloud sync completes.

## Problem

Users need a proper way to exit games to ensure data integrity.

## Scope

- In scope:
  - Exit option in overlay menu
  - Graceful game shutdown
  - Save game progress
  - Cloud save sync on exit
  - Container cleanup

## Acceptance Criteria

- [ ] User can exit game from overlay
- [ ] Game progress is saved
- [ ] Cloud saves sync on exit
- [ ] Container properly shuts down
- [ ] User returns to library/app

## Validation

- [ ] Manual flow: Exit game properly
- [ ] Manual flow: Verify saves synced