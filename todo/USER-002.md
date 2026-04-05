# USER-002 - Connect Steam Account

- **ID**: `USER-002`
- **Area**: `authentication/steam`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, I want to connect my Steam account to the app so that I can access my Steam library and play my games.

## Problem

Users need to authenticate with Steam to access their game library, but the login flow may not be clear or may have issues.

## Scope

- In scope:
  - Steam login via QR code (preferred method)
  - Steam login via username/password as fallback
  - Two-factor authentication handling
  - Session persistence
  - Clear error messages on auth failure
  - "Connected" confirmation with account display

## Acceptance Criteria

- [ ] User can initiate Steam connection from settings or library screen
- [ ] QR code login works and is the primary method shown
- [ ] Username/password login available as alternative
- [ ] 2FA codes can be entered when required
- [ ] Successful login shows confirmation with account info
- [ ] Failed login shows clear error message
- [ ] Session persists across app restarts
- [ ] User can view which Steam account is connected

## Validation

- [ ] Manual flow: Connect valid Steam account
- [ ] Manual flow: Handle 2FA when required
- [ ] Manual flow: Failed login shows appropriate error