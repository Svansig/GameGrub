# ARCH-006e - Refactor AmazonAppScreen to Use Enhanced Base

- **ID**: `ARCH-006e`
- **Area**: `ui/screen/library`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

AmazonAppScreen has duplicated logic that should be in base class.

## Scope

- In scope:
  - Refactor AmazonAppScreen to use enhanced BaseAppScreen
  - Reduce overridden methods
  - Keep only Amazon-specific logic
- Out of scope:
  - Other screens

## Dependencies and Decomposition

- Parent ticket: `ARCH-006`
- Child tickets: 
  - `ARCH-006a` (must complete first)
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-006a`

## Acceptance Criteria

- [ ] AmazonAppScreen uses enhanced base
- [ ] 50% reduction in lines of code
- [ ] All functionality preserved

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] Manual: View Amazon game details
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
