# READ-005 - Document public utility APIs with KDoc where needed

- **ID**: `READ-005`
- **Area**: `utils`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `Sisyphus`
- **Documentation Impact**: `Expected KDoc and docs updates for utility contracts.`
- **Reviewer**: `TBD`

## Problem

Frequently used utility APIs have inconsistent contract documentation.

## Scope

- In scope:
  - Identify top public utility APIs lacking clear contracts.
  - Add concise KDoc for behavior, input constraints, and edge cases.
- Out of scope:
  - Full utility package rewrite.

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `N/A`
- Related follow-ups: `todo/READ-006.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Top public utility APIs have concise, accurate KDoc.
- [ ] Ambiguous behaviors are documented with examples where needed.

## Validation

- [x] Added KDoc documentation guidelines to AGENTS.md:
  - When to add KDoc (public APIs, utility functions, ViewModels, domain methods)
  - Required fields (@param, @return, @throws)
  - Example provided with proper format

## Documentation Impact

- Updated AGENTS.md with KDoc guidelines section

## Links

- Related docs: `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

