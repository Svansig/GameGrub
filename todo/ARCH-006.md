# ARCH-006 - Unified AppScreen Base Class Enhancement

- **ID**: `ARCH-006`
- **Area**: `ui/screen/library`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

BaseAppScreen already exists but:
- Each store still overrides many methods with similar code
- GameDisplayInfo construction duplicated in each subclass
- Store-specific logic scattered across subclasses

## Scope

- In scope:
  - Enhance BaseAppScreen to reduce subclass overrides
  - Move common GameDisplayInfo logic to base
  - Extract store-specific hooks cleanly
  - Add new stores should only require minimal overrides
- Out of scope:
  - Service layer changes

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: 
  - `ARCH-006a` - Analyze BaseAppScreen and identify consolidation opportunities
  - `ARCH-006b` - Refactor SteamAppScreen to use enhanced base
  - `ARCH-006c` - Refactor GOGAppScreen to use enhanced base
  - `ARCH-006d` - Refactor EpicAppScreen to use enhanced base
  - `ARCH-006e` - Refactor AmazonAppScreen to use enhanced base
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-001` (depends on unified game model)

## Acceptance Criteria

- [ ] BaseAppScreen handles more common logic
- [ ] Each subclass is 50% smaller
- [ ] Adding new store requires minimal code

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: View game detail page for each store
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
