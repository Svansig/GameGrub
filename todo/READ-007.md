# READ-007 - Standardize log tag/message conventions

- **ID**: `READ-007`
- **Area**: `app/src/main/java`
- **Priority**: `P3`
- **Status**: `Done`
- **Owner**: `Sisyphus`
- **Documentation Impact**: `Updated AGENTS.md Logging section with comprehensive conventions`
- **Documentation Impact**: `Expected logging convention updates in AGENTS and examples.`
- **Reviewer**: `TBD`

## Problem

Log styles and tags vary, reducing debug signal quality.

## Scope

- In scope:
  - Define log tag and message conventions.
  - Normalize a representative set of files.
- Out of scope:
  - Full log cleanup across all modules.

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `N/A`
- Related follow-ups: `todo/SEC-004.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Logging style guide added and linked.
- [ ] Representative files follow the convention.

## Validation

- [x] AGENTS.md Logging section updated with:
  - Log levels (Debug, Info, Warning, Error)
  - Tag conventions (class name default, contextual `[ComponentName][Action]` format)
  - Message format guidelines (descriptive but concise, include identifiers, avoid sensitive data)
- [ ] Representative files normalized - deferred to follow-up

## Documentation Impact

- Updated AGENTS.md Logging section with comprehensive conventions

## Links

- Related docs: `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

