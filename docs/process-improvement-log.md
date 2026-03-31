# Process Improvement Log

Use this running log to capture opportunities discovered while implementing or reviewing tickets.

## Entry Format

- **Date**: `YYYY-MM-DD`
- **Ticket**: `todo/<ID>.md`
- **PR/Commit**: `<link or sha>`
- **Type**: `Code Quality | Workflow Quality | Tooling | Documentation`
- **Opportunity**: `<what could be improved>`
- **Proposed Action**: `<small concrete change>`
- **Owner**: `<name or TBD>`
- **Status**: `Backlog | In Progress | Done`

## Entries

- **Date**: `2026-03-31`
- **Ticket**: `N/A (compile stabilization pass)`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Compile-only regressions were introduced during service/domain refactors without a fast guard for cross-file API moves.
- **Proposed Action**: Add a lightweight CI/local gate that always runs `:app:compileDebugKotlin` after Steam refactor slices.
- **Owner**: `TBD`
- **Status**: `Backlog`

- **Date**: `2026-03-31`
- **Ticket**: `todo/UI-004.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: OAuth callback parsing logic was duplicated across composables and difficult to unit test.
- **Proposed Action**: Keep auth-result parsing in small pure helpers with dedicated tests before wiring side effects in ViewModel.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (local dedup cleanup)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `BestConfigService` repeated key-presence checks and driver-version parsing logic in multiple branches.
- **Proposed Action**: Centralize JSON field extraction and parsing in private helpers to cut copy/paste paths and reduce drift risk.
- **Owner**: `TBD`
- **Status**: `Done`

