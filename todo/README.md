# Local Ticket System

This directory is the project-local ticket tracker for work that needs coordination but does not rely on external issue tracking.

## Why this exists

- Keep active engineering work visible in the repo.
- Make ticket context available to contributors and coding agents.
- Link plans and docs directly to actionable tasks.

## Current Program Goal

The current program goal is **refactor-first**: make the codebase more manageable, cohesive, and maintainable.

Until this goal is explicitly changed, prioritize tickets that directly reduce complexity, improve boundaries, and improve testability/readability of existing behavior.

## Ticket Acceptance Filter (Refactor Phase)

Create or prioritize tickets only when they primarily do one or more of the following:

- Reduce architectural coupling or responsibility sprawl.
- Improve code organization, naming, readability, and ownership clarity.
- Improve reliability/testability of existing flows without broad feature expansion.
- Improve docs/process specifically to support refactor execution.

During this phase, avoid introducing new feature scope unless it is required to complete refactor acceptance criteria.

## Ticket ID and file naming

- Use existing area IDs when available (for example `UI-001`).
- Store each ticket as one Markdown file at `todo/<ID>.md`.
- Keep IDs stable after creation.

### Prefix taxonomy

- `UI-xxx`: UI placement, boundary, and UX architecture work.
- `COH-xxx`: cohesion and layering improvements.
- `READ-xxx`: readability and maintainability improvements.
- `PERF-xxx`: performance and resource usage improvements.
- `REL-xxx`: reliability and failure-handling improvements.
- `TEST-xxx`: test coverage and regression confidence improvements.
- `CI-xxx`: build, CI, and release pipeline improvements.
- `SEC-xxx`: security hardening and dependency risk reduction.
- `DOC-xxx`: documentation quality and discoverability improvements.

## Ticket lifecycle

- `Backlog`: triaged, not started.
- `In Progress`: someone is actively working on it.
- `Blocked`: cannot proceed due to dependency/decision.
- `Done`: merged and validated.

## Required ticket fields

Each ticket should include:

- ID
- Title
- Area
- Priority
- Status
- Problem statement
- Acceptance criteria
- Validation plan
- Documentation impact
- Links (docs, PRs, commits)

Use `todo/TICKET_TEMPLATE.md` for new tickets.

## Working agreement

1. Pick from `todo/INDEX.md` in priority order unless there is a release-driven override.
2. Move status to `In Progress` before coding.
3. Keep acceptance criteria and validation evidence updated in the ticket.
4. Update docs as part of the change and record doc impact in the ticket.
5. Commit implementation changes before requesting review.
6. Request independent review from a different agent/reviewer.
7. Commit all post-review updates.
8. Record improvement opportunities in `docs/process-improvement-log.md`.
9. Move to `Done` only after merge + validation.

## Ticket decomposition and continuity

- If a ticket is too large or complex, split it into additional tickets immediately and cross-link parent/child tickets.
- If split tickets are required for the current ticket to reach acceptance, begin those tickets immediately.
- If implementation uncovers new required work, create the new ticket(s) immediately and link them before continuing.
- Do not abandon `In Progress` tickets; either complete them or mark `Blocked` with a concrete blocker and linked follow-up tickets.

## Backlog hygiene

- Keep `todo/INDEX.md` sorted by priority inside each theme.
- Add links to related plans/docs in every ticket.
- If a ticket is large, split it before implementation starts.
- Prefer small, mergeable increments with explicit acceptance criteria.
- For older tickets missing `Documentation impact`, add it when the ticket is first touched.

