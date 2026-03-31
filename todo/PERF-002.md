# PERF-002 - Audit Compose recomposition hotspots

- **ID**: `PERF-002`
- **Area**: `ui`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Potential unnecessary recompositions may degrade UI smoothness and battery usage.

## Scope

- In scope:
  - Identify high-frequency recomposition hotspots.
  - Apply targeted state-hoisting and memoization improvements.
- Out of scope:
  - Complete UI rewrite.

## Acceptance Criteria

- [ ] Top 5 hotspots identified with evidence.
- [ ] At least 2 hotspots improved with measurable reduction.
- [ ] No functional UI regressions introduced.

## Validation

- [ ] Manual sanity checks for affected screens.

## Links

- Related docs: `docs/ui-placement/ui-migration-playbook.md`
- PR: `TBD`

