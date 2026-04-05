# USER-046 - Launch Game via Deep Link

- **ID**: `USER-046`
- **Area**: `launch`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to launch the app and a specific game from an external link so that I can quickly start playing from notifications or other apps.

## Problem

Users may receive notifications or links that should launch directly into a game.

## Scope

- In scope:
  - Handle game:// or similar deep links
  - Parse game ID from link
  - Auto-launch game if installed
  - Prompt to install if not installed

## Acceptance Criteria

- [ ] Deep link opens correct game
- [ ] Installed game launches automatically
- [ ] Not installed game shows prompt to install

## Validation

- [ ] Manual flow: Use deep link to launch game