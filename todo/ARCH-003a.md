# ARCH-003a - Design and Implement BaseLaunchCommandBuilder

- **ID**: `ARCH-003a`
- **Area**: `container/launch`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Need base class to eliminate duplicated launch command building logic.

## Scope

- In scope:
  - Analyze all existing launch command builders
  - Design abstract base with common flow:
    - Container initialization
    - Drive mapping
    - Environment variables
    - Working directory setup
    - Command prefix/suffix
  - Define interface for store-specific command building
- Out of scope:
  - Individual builder migrations (child tickets)

## Dependencies and Decomposition

- Parent ticket: `ARCH-003`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Analysis of existing launch builders
- [ ] `BaseLaunchCommandBuilder` with template methods
- [ ] Store-specific interface defined
- [ ] Design documentation

## Validation

- [ ] Design reviewed
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
