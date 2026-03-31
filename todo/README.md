# Local Ticket System

This directory is the project-local ticket tracker for work that needs coordination but does not rely on external issue tracking.

## Why this exists

- Keep active engineering work visible in the repo.
- Make ticket context available to contributors and coding agents.
- Link plans and docs directly to actionable tasks.

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
- Links (docs, PRs, commits)

Use `todo/TICKET_TEMPLATE.md` for new tickets.

## Working agreement

1. Pick from `todo/INDEX.md` in priority order unless there is a release-driven override.
2. Move status to `In Progress` before coding.
3. Keep acceptance criteria and validation evidence updated in the ticket.
4. Move to `Done` only after merge + validation.

## Backlog hygiene

- Keep `todo/INDEX.md` sorted by priority inside each theme.
- Add links to related plans/docs in every ticket.
- If a ticket is large, split it before implementation starts.
- Prefer small, mergeable increments with explicit acceptance criteria.

