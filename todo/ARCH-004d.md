# ARCH-004d - Migrate Amazon Auth to Interface

- **ID**: `ARCH-004d`
- **Area**: `service/auth`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Amazon auth should implement unified interface.

## Scope

- In scope:
  - Refactor AmazonAuthManager to implement GameStoreAuth
  - Ensure same auth flow works
- Out of scope:
  - Other stores

## Dependencies and Decomposition

- Parent ticket: `ARCH-004`
- Child tickets: 
  - `ARCH-004a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-004a`

## Acceptance Criteria

- [ ] AmazonAuthManager implements GameStoreAuth
- [ ] Login/logout works for Amazon

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Login to Amazon, verify works
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
