# USER-004 - Connect Epic Games Account

- **ID**: `USER-004`
- **Area**: `authentication/epic`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to connect my Epic Games account to the app so that I can access my Epic library and play my games.

## Problem

Users need to authenticate with Epic to access their game library.

## Scope

- In scope:
  - Epic OAuth login flow
  - Session persistence
  - Clear error messages on auth failure
  - "Connected" confirmation with account display

## Acceptance Criteria

- [ ] User can initiate Epic connection
- [ ] OAuth flow completes successfully
- [ ] Successful login shows confirmation with account info
- [ ] Failed login shows clear error message
- [ ] Session persists across app restarts
- [ ] User can view which Epic account is connected

## Validation

- [ ] Manual flow: Connect valid Epic account
- [ ] Manual flow: Handle auth failure gracefully