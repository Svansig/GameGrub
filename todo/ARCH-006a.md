# ARCH-006a - Analyze BaseAppScreen Consolidation Opportunities

- **ID**: `ARCH-006a`
- **Area**: `ui/screen/library`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Need to identify what can be moved from subclasses to base class.

## Scope

- In scope:
  - Analyze all AppScreen subclasses (Steam, GOG, Epic, Amazon, Custom)
  - Document common vs store-specific code
  - Identify methods that can be moved to base
  - Design enhanced base class
- Out of scope:
  - Implementation changes

## Dependencies and Decomposition

- Parent ticket: `ARCH-006`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Analysis document of each subclass
- [ ] List of methods to move to base
- [ ] Design for enhanced BaseAppScreen

## Validation

- [ ] Design reviewed
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
