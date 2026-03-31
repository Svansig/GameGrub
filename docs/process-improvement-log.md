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

