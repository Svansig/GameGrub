# ARCH-004 - Unified Authentication Abstraction

- **ID**: `ARCH-004`
- **Area**: `service/auth`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each store has separate auth handling:
- GOGAuthManager
- EpicAuthManager
- AmazonAuthManager
- Steam's built-in authentication via JavaSteam

This creates:
- Duplicated OAuth flow code
- Inconsistent token storage
- Hard to add common auth features (session refresh, logout)

## Scope

- In scope:
  - Create unified `GameStoreAuth` interface
  - Abstract common OAuth/token operations
  - Unified token storage (or clearly document differences)
  - Migrate all auth managers to interface
- Out of scope:
  - UI changes for login screens

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-004a` - Define GameStoreAuth interface
  - `ARCH-004b` - Migrate GOG auth to interface
  - `ARCH-004c` - Migrate Epic auth to interface
  - `ARCH-004d` - Migrate Amazon auth to interface
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-002` (depends on service layer refactor)

## Acceptance Criteria

- [ ] `GameStoreAuth` interface with: login, logout, refreshToken, isLoggedIn, getCredentials
- [ ] All 4 stores implement interface (Steam wraps JavaSteam)
- [ ] Common token storage where possible
- [ ] Auth flow works for all stores

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual: Login to each store, verify tokens stored
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
