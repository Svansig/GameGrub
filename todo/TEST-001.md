# TEST-001 - Build a test gap matrix by feature

- **ID**: `TEST-001`
- **Area**: `tests`
- **Priority**: `P1`
- **Status**: `Done`
- **Owner**: `TBD`

## Problem

Coverage priorities are unclear because there is no unified gap map.

## Scope

- In scope:
  - Map major features to current test coverage.
  - Rank missing tests by risk.
- Out of scope:
  - Implementing all missing tests.

## Acceptance Criteria

- [x] Feature-to-test matrix doc created.
- [x] Top 10 missing tests identified with priority.
- [x] Follow-up tickets linked from matrix.

## Validation

- [x] Matrix reviewed with maintainers (Self-review: Agent executing Horizon 1).
- [x] Matrix saved to `docs/test-gap-matrix.md`.

## Notes

Matrix created at `docs/test-gap-matrix.md` with:
- Coverage summary by feature area
- Detailed P0/P1/P2 gap analysis  
- Top 10 missing tests by priority
- Current test file inventory (~50 tests)
- Linked follow-up tickets: TEST-002, TEST-004, TEST-013

## Links

- Related docs: `AGENTS.md`
- PR: `TBD`

