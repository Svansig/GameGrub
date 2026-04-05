# USER-005 - Connect Amazon Games Account

- **ID**: `USER-005`
- **Area**: `authentication/amazon`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to connect my Amazon Games account to the app so that I can access my Amazon library and play my games.

## Problem

Users need to authenticate with Amazon to access their game library.

## Scope

- In scope:
  - Amazon login flow (PKCE)
  - Session persistence
  - Clear error messages on auth failure
  - "Connected" confirmation with account display

## Acceptance Criteria

- [ ] User can initiate Amazon connection
- [ ] Login flow completes successfully
- [ ] Successful login shows confirmation with account info
- [ ] Failed login shows clear error message
- [ ] Session persists across app restarts
- [ ] User can view which Amazon account is connected

## Validation

- [ ] Manual flow: Connect valid Amazon account
- [ ] Manual flow: Handle auth failure gracefully