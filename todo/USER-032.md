# USER-032 - Handle Authentication Failure

- **ID**: `USER-032`
- **Area**: `error handling`
- **Priority**: `P0`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required`
- **Reviewer**: `TBD`

## User Story

As a user, when authentication fails I want to see a clear error message so that I know what went wrong and how to fix it.

## Problem

Auth may fail due to invalid credentials, network issues, or account problems.

## Scope

- In scope:
  - Clear error message about auth failure reason
  - Retry option
  - Help link or suggestions for common issues
  - Session expiry handling

## Acceptance Criteria

- [ ] Clear error message shown on auth failure
- [ ] User can retry authentication
- [ ] Common issues are explained
- [ ] Session expiry is handled gracefully

## Validation

- [ ] Manual flow: Trigger auth failure
- [ ] Manual flow: Retry successfully