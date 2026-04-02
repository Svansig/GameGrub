# ARCH-006c - Refactor GOGAppScreen to Use Enhanced Base

- **ID**: `ARCH-006c`
- **Area**: `ui/screen/library`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

GOGAppScreen has duplicated logic that should be in base class.

## Scope

- In scope:
  - Refactor GOGAppScreen to use enhanced BaseAppScreen
  - Reduce overridden methods
  - Keep only GOG-specific logic
- Out of scope:
  - Other screens

## Dependencies and Decomposition

- Parent ticket: `ARCH-006`
- Child tickets: 
  - `ARCH-006a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-006a`

## Acceptance Criteria

- [ ] GOGAppScreen uses enhanced base
- [ ] 50% reduction in lines of code
- [ ] All functionality preserved

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: View GOG game details
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
