# USER-003 - Connect GOG Account

- **ID**: `USER-003`
- **Area**: `authentication/gog`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to connect my GOG account to the app so that I can access my GOG library and play my games.

## Problem

Users need to authenticate with GOG to access their game library.

## Scope

- In scope:
  - GOG OAuth login flow
  - Session persistence
  - Clear error messages on auth failure
  - "Connected" confirmation with account display

## Acceptance Criteria

- [ ] User can initiate GOG connection
- [ ] OAuth flow completes successfully
- [ ] Successful login shows confirmation with account info
- [ ] Failed login shows clear error message
- [ ] Session persists across app restarts
- [ ] User can view which GOG account is connected

## Validation

- [ ] Manual flow: Connect valid GOG account
- [ ] Manual flow: Handle auth failure gracefully