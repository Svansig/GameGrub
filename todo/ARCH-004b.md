# ARCH-004b - Migrate GOG Auth to Interface

- **ID**: `ARCH-004b`
- **Area**: `service/auth`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

GOG auth should implement unified interface.

## Scope

- In scope:
  - Refactor GOGAuthManager to implement GameStoreAuth
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

- [ ] GOGAuthManager implements GameStoreAuth
- [ ] Login/logout works for GOG

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: Login to GOG, verify works
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
