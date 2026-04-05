# USER-055 - Enable/Disable Auto-Start

- **ID**: `USER-055`
- **Area**: `settings/system`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to configure whether the app auto-starts when my device boots so that I can manage background services.

## Problem

Users may want the app to start on boot or may not want it to.

## Scope

- In scope:
  - Auto-start toggle in settings
  - Boot receiver registration

## Acceptance Criteria

- [ ] User can enable auto-start on boot
- [ ] User can disable auto-start on boot
- [ ] Setting persists

## Validation

- [ ] Manual flow: Toggle auto-start
- [ ] Manual flow: Reboot device and verify