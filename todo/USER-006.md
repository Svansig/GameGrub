# USER-006 - View Game Library

- **ID**: `USER-006`
- **Area**: `library`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to see all my games from connected stores in one unified library view so that I can easily browse and decide what to play.

## Problem

Users connect multiple store accounts but need a consolidated view of all their games.

## Scope

- In scope:
  - Combined library view across all connected stores
  - Visual distinction between stores (Steam, GOG, Epic, Amazon)
  - Game artwork/box art display
  - Game title and store display
  - Installed vs not installed indicator
  - Pull-to-refresh for library sync

## Acceptance Criteria

- [ ] Library shows games from all connected accounts
- [ ] Each game displays title and store source
- [ ] Each game displays artwork/box art
- [ ] Installed games visually distinguished from not installed
- [ ] User can pull-to-refresh to sync library
- [ ] Loading state shown during initial fetch
- [ ] Empty state shown when no accounts connected

## Validation

- [ ] Manual flow: View library with connected accounts
- [ ] Manual flow: Verify games from multiple stores appear
- [ ] Manual flow: Verify installed status is accurate