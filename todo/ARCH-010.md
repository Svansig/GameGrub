# ARCH-010 - Remove Static Utility Classes

- **ID**: `ARCH-010`
- **Area**: `utils`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Many utility classes use static methods instead of injectable instances. This makes testing difficult and creates tight coupling.

## Scope

- In scope:
  - Identify all static utility classes
  - Convert to injectable instances where appropriate
  - Move static utilities to extensions or proper utils
- Out of scope:
  - Core platform utilities

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Key utilities are injectable
- [ ] Static methods reduced in utils packages

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
