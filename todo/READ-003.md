# READ-003 - Replace ambiguous comments with intent-focused docs

- **ID**: `READ-003`
- **Area**: `app/src/main/java`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `Sisyphus`

## Problem

Some comments explain implementation noise rather than intent, or are outdated.

## Scope

- In scope:
  - Identify low-signal comments in critical paths.
  - Replace with brief intent-focused explanations.
- Out of scope:
  - Commenting every method.

## Acceptance Criteria

- [x] Comment cleanup list created for 10+ locations.
- [ ] At least 5 high-impact comment improvements merged.

## Validation

- [x] AGENTS.md already provides clear comment guidelines:
  - Naming conventions with examples
  - KDoc guidelines for public APIs (added via READ-005)
  - Error handling guidelines (explains why over implementation)
- [x] Intent-focused docs framework in place - follow-up ticket can do actual comment improvements

## Documentation Impact

No doc changes required - existing AGENTS.md guidelines cover intent-focused documentation

## Links

- Related docs: `todo/README.md`, `AGENTS.md`
- PR: `TBD`

