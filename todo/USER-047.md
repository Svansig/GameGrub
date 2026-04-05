# USER-047 - Handle App Background/Foreground

- **ID**: `USER-047`
- **Area**: `lifecycle`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want the app to handle being backgrounded and foregrounded gracefully so that my downloads and running games continue to work.

## Problem

The app needs to manage state when user switches to other apps.

## Scope

- In scope:
  - Continue downloads in background
  - Keep running game active
  - Background service notification
  - Return to same state when foregrounding

## Acceptance Criteria

- [ ] Downloads continue when app is backgrounded
- [ ] Running game continues when app is backgrounded
- [ ] User returns to same screen on foreground
- [ ] State is preserved correctly

## Validation

- [ ] Manual flow: Start download, background app
- [ ] Manual flow: Launch game, switch apps, return