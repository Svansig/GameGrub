# UI Placement Documentation

This documentation set supports the effort to identify and fix UI elements that do not belong in their current layer, package, or responsibility boundary.

## Goals

- Provide one working source of truth for UI cleanup scope and status.
- Keep fixes incremental and safe (behavior-preserving migrations first).
- Make ownership, review expectations, and validation explicit.

## How to Use This Package

1. Start in `todo/INDEX.md` to pick the next ticket.
2. Open the linked `todo/UI-xxx.md` ticket for acceptance criteria and validation scope.
3. Use `docs/ui-placement/ui-element-triage-register.md` for architectural context and rationale.
4. Validate destination in `docs/ui-placement/ui-target-structure-map.md`.
5. Follow execution steps in `docs/ui-placement/ui-migration-playbook.md`.
6. Apply checks in `docs/ui-placement/ui-validation-checklist.md` before merge.
7. Keep owner and reviewer assignments updated in `docs/ui-placement/ui-ownership-matrix.md`.

## Local Ticket System

- Ticket root: `todo/`
- Backlog board: `todo/INDEX.md`
- Ticket format: `todo/TICKET_TEMPLATE.md`
- Current UI cleanup tickets: `todo/UI-001.md` to `todo/UI-009.md`

When triage register items change, update the matching ticket file in `todo/` in the same PR.

## Document Map

- `docs/ui-placement/ui-element-triage-register.md`: prioritized inventory of misplaced UI elements.
- `docs/ui-placement/ui-target-structure-map.md`: canonical current-vs-target placement map and rules.
- `docs/ui-placement/ui-migration-playbook.md`: phased migration process and PR execution recipe.
- `docs/ui-placement/ui-validation-checklist.md`: regression and quality gates.
- `docs/ui-placement/ui-ownership-matrix.md`: accountability model for triage through merge.

## Scope Boundaries

In scope:
- `app/src/main/java/app/gamegrub/ui/**`
- UI-to-service coupling and UI-to-legacy coupling where it leaks business logic.
- UI-facing documentation drift (for example, incorrect folder names in architecture docs).

Out of scope (unless needed to unblock a UI placement fix):
- broad service redesign not directly required by a UI move,
- non-UI subsystems unrelated to ownership boundaries.

## Priority Scale

- `P0`: blocks releases or causes user-facing breakage/regression risk now.
- `P1`: high-impact architecture/lifecycle risk; should be addressed soon.
- `P2`: maintainability/tooling issues causing repeated friction.
- `P3`: hygiene/consistency improvements.

## Status Conventions

- `Backlog`: triaged, not started.
- `In Progress`: active branch/PR exists.
- `Blocked`: waiting on dependency/decision.
- `Done`: merged, validated with checklist evidence.
