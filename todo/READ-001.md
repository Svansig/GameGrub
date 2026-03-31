# READ-001 - Split oversized Kotlin files by responsibility

- **ID**: `READ-001`
- **Area**: `ui + service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Very large files reduce readability, reviewability, and safe iteration speed.

## Scope

- In scope:
  - Inventory oversized files and split candidates.
  - Start with top 3 high-impact files.
- Out of scope:
  - Functional redesign during split.

## Acceptance Criteria

- [ ] Oversized file list produced with target split plan.
- [ ] At least one high-impact file split without behavior change.
- [ ] New file boundaries follow project conventions.

## Validation

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintKotlin`

## Links

- Related docs: `docs/ui-placement/ui-migration-playbook.md`
- PR: `TBD`

