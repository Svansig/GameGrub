# USER-017 - Sync Cloud Saves

- **ID**: `USER-017`
- **Area**: `cloud/saves`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want my game saves to sync to the cloud so that I can continue playing on different devices or after reinstallation.

## Problem

Players want their progress backed up and available across sessions.

## Scope

- In scope:
  - Automatic cloud save sync
  - Manual sync trigger
  - Sync status indicator
  - Conflict resolution
  - Force download remote saves option
  - Force upload local saves option

## Acceptance Criteria

- [ ] Saves sync automatically when game closes
- [ ] User can manually trigger sync
- [ ] User can see sync status
- [ ] User can force download remote saves
- [ ] User can force upload local saves
- [ ] Conflicts are handled gracefully

## Validation

- [ ] Manual flow: Play game, verify saves sync
- [ ] Manual flow: Manual sync trigger
- [ ] Manual flow: Force save operations