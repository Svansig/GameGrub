# ARCH-001b - Create Room Migration for Unified Games Table

- **ID**: `ARCH-001b`
- **Area**: `db`
- **Priority**: `P0`
- **Status**: `In Progress`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Current database has separate tables for each store with different schemas. Need migration path to unified table.

## Scope

- In scope:
  - Create new unified `games` table with GameSource discriminator
  - Add nullable columns for store-specific data
  - Create migration from legacy tables
  - Preserve existing install status and paths
- Out of scope:
  - Application code changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-001`
- Child tickets: 
  - `ARCH-001a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001a`

## Acceptance Criteria

- [ ] New `games` table created with all common fields
- [ ] GameSource enum column as discriminator
- [ ] Store-specific JSON blob column for extended data
- [ ] Migration script preserves: isInstalled, installPath, installSize, lastPlayed, playTime
- [ ] Rollback procedure documented

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Migration tested on fresh DB and existing DB
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
