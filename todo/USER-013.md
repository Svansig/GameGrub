# USER-013 - Uninstall Game

- **ID**: `USER-013`
- **Area**: `maintenance`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to uninstall a game so that I can free up storage space.

## Problem

Users need to remove installed games to free up device storage.

## Scope

- In scope:
  - Uninstall option in game options
  - Confirmation dialog before uninstall
  - Remove game files from storage
  - Clean up associated data (optional: keep saves)
  - Uninstall progress (if large game)

## Acceptance Criteria

- [ ] User can initiate uninstall from game options
- [ ] Confirmation dialog prevents accidental uninstall
- [ ] Game files are removed from storage
- [ ] User can choose to keep or remove cloud saves
- [ ] Game removed from "Installed" list
- [ ] Storage space is freed

## Validation

- [ ] Manual flow: Uninstall installed game
- [ ] Manual flow: Verify files removed and space freed