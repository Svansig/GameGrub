# ARCH-019 - Migrate Launch Builders to Base Class

- **ID**: `ARCH-019`
- **Area**: `container/launch`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Launch builders duplicate common logic.

## Scope

- In scope:
  - Refactor SteamLaunchCommandBuilder to extend BaseLaunchCommandBuilder
  - Refactor GogLaunchCommandBuilder to extend BaseLaunchCommandBuilder
  - Refactor EpicLaunchCommandBuilder to extend BaseLaunchCommandBuilder
  - Refactor AmazonLaunchCommandBuilder to extend BaseLaunchCommandBuilder
- Out of scope:
  - Container changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-003a`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] All launch builders extend BaseLaunchCommandBuilder
- [ ] 30% reduction in duplicate launch logic

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
