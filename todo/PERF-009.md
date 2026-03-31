# PERF-009 - Add baseline profiling for auth and sync workflows

- **ID**: `PERF-009`
- **Area**: `service/auth + service/steam`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected profiling baseline docs update.`
- **Reviewer**: `TBD`

## Problem

Auth and sync workflow performance lacks baseline measurements.

## Scope

- In scope:
  - Capture baseline timings for auth and sync pathways.
  - Document bottleneck candidates.
- Out of scope:
  - Major optimization effort in same ticket.

## Dependencies and Decomposition

- Parent ticket: `todo/PERF-001.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/PERF-010.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Baseline measurements captured for target flows.
- [ ] Bottlenecks and next actions documented.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] Profiling method is reproducible.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `docs/process-improvement-log.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

