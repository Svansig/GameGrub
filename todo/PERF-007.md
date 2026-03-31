# PERF-007 - Reduce allocations in input and rendering hot paths

- **ID**: `PERF-007`
- **Area**: `xserver + input`
- **Priority**: `P3`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Expected profiling notes and hotspot findings updates.`
- **Reviewer**: `TBD`

## Problem

Hot-path object churn can increase GC pressure and frame instability.

## Scope

- In scope:
  - Profile allocation-heavy loops in input/render paths.
  - Apply safe micro-optimizations.
- Out of scope:
  - Major renderer rewrite.

## Dependencies and Decomposition

- Parent ticket: `todo/PERF-001.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/REL-007.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Allocation hotspots identified and documented.
- [ ] At least one hotspot improved with measurable effect.
- [ ] Documentation updated (or `No doc changes required` note added with reason).

## Validation

- [ ] Profiling evidence captured before/after.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `todo/PERF-001.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

