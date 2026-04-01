# READ-002 - Normalize naming for manager/coordinator/domain classes

- **ID**: `READ-002`
- **Area**: `service`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `Sisyphus`

## Problem

Naming patterns are inconsistent, making responsibilities harder to infer.

## Scope

- In scope:
  - Define naming rules for manager/coordinator/domain/use-case classes.
  - Rename ambiguous classes incrementally.
- Out of scope:
  - Broad package moves unrelated to naming.

## Acceptance Criteria

- [x] Naming guide section added in docs.
- [ ] Initial rename batch completed with migration notes.
- [ ] No unresolved references or behavior changes.

## Validation

- [x] Naming guide added to AGENTS.md (Architecture Role Naming table)
- [ ] Initial rename batch - deferred to follow-up tickets

## Documentation Impact

- Updated AGENTS.md with Architecture Role Naming table (Domain, Manager, Coordinator, Gateway, UseCase)

## Links

- Related docs: `AGENTS.md`
- PR: `TBD`

