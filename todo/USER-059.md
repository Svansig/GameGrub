# USER-059 - Handle Insufficient Storage

- **ID**: `USER-059`
- **Area**: `error handling`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, when there is insufficient storage to install a game, I want to be warned and given options to free space so that I can complete the installation.

## Problem

Users may try to install games without enough storage space.

## Scope

- In scope:
  - Pre-check storage before download starts
  - Clear error message about insufficient space
  - Show how much space is needed vs available
  - Quick options to free space (clear cache, uninstall other games)

## Acceptance Criteria

- [ ] User is warned before download starts
- [ ] Error clearly shows space needed
- [ ] User can access quick cleanup options
- [ ] User can choose to proceed anyway

## Validation

- [ ] Manual flow: Attempt download with low space