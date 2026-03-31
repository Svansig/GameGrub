# READ-002 - Normalize naming for manager/coordinator/domain classes

- **ID**: `READ-002`
- **Area**: `service`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Naming patterns are inconsistent, making responsibilities harder to infer.

## Scope

- In scope:
  - Define naming rules for manager/coordinator/domain/use-case classes.
  - Rename ambiguous classes incrementally.
- Out of scope:
  - Broad package moves unrelated to naming.

## Acceptance Criteria

- [ ] Naming guide section added in docs.
- [ ] Initial rename batch completed with migration notes.
- [ ] No unresolved references or behavior changes.

## Validation

- [ ] `./gradlew testDebugUnitTest`

## Links

- Related docs: `AGENTS.md`
- PR: `TBD`

