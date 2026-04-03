# ARCH-001c - Update DAO and Repository Layer

- **ID**: `ARCH-001c`
- **Area**: `db`
- **Priority**: `P0`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Need unified data access layer that abstracts away store-specific differences.

## Scope

- In scope:
  - Create unified `GameDao` interface
  - Implement with GameSource filtering
  - Add repository abstraction for business logic
  - Update service layer to use unified DAO
- Out of scope:
  - UI layer changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-001`
- Child tickets: 
  - `ARCH-001b` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001b`

## Acceptance Criteria

- [ ] `GameDao` interface with: getAll, getById, getBySource, getInstalled, search, upsert
- [ ] `GameRepository` class with business logic
- [ ] Backward-compatible reads from legacy tables during migration
- [ ] All services updated to use unified DAO

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] All store libraries load correctly after migration
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
