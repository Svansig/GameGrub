# ARCH-001 - Unified Game Domain Model

- **ID**: `ARCH-001`
- **Area**: `data/domain`
- **Priority**: `P0`
- **Status**: `In Progress`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor, no user-facing docs
- **Reviewer**: `TBD`

## Problem

Each game store has its own data entity (SteamApp, GOGGame, EpicGame, AmazonGame) with nearly identical fields (name, icon, installPath, isInstalled, size, playTime, etc.). This causes:
- Duplicated fields across 4 entities
- No single source of truth for game metadata
- Complex conversion logic when converting between types
- Harder to add new stores (must duplicate all common fields)

## Scope

- In scope:
  - Create unified `Game` interface in `app.gamegrub.data`
  - Add store-specific data to a single game entity using discriminator field
  - Migrate to single `Games` table in Room with nullable store-specific fields
  - Update all consumers to use unified model
- Out of scope:
  - Service layer changes (covered in ARCH-002)
  - UI layer changes (covered in ARCH-003)
  - Launch command changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-001a` - Define unified Game interface and entity schema
  - `ARCH-001b` - Create Room migration for unified table
  - `ARCH-001c` - Update DAO and repository layer
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Unified `Game` interface created with common fields
- [ ] Store-specific fields handled via nullable fields or JSON extension
- [ ] Room database has single games table with GameSource discriminator
- [ ] All reads/writes go through unified DAO
- [ ] Existing entities optionally deprecated/migrated
- [ ] No runtime regressions for existing features

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Manual flow checks captured - Launch Steam/GOG/Epic/Amazon game
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
