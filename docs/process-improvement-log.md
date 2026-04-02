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

- **Date**: `2026-04-01`
- **Ticket**: `todo/REL-015.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Orientation updates were emitted from composable body execution, creating avoidable recomposition-driven event churn.
- **Proposed Action**: Require orientation/system side effects to be emitted from keyed effect blocks (`LaunchedEffect`/lifecycle coordinators) and add lint guidance for event emissions in composable bodies.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `todo/TEST-017.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Local verification for orientation tickets can be blocked when `JAVA_HOME` is missing, delaying regression validation.
- **Proposed Action**: Add a short contributor runbook note for Java setup validation before Gradle tasks (`java -version`, `JAVA_HOME`) and include it in troubleshooting docs.
- **Owner**: `TBD`
- **Status**: `Backlog`

- **Date**: `2026-04-01`
- **Ticket**: `N/A (Steam PICS sync stabilization)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Long-lived Steam sync jobs were started without stored Job references, making cancellation ineffective and allowing duplicate background loops after reconnects.
- **Proposed Action**: Require every continuous domain worker to store/cancel its active `Job` in one place and add a reconnect regression test for duplicate workers.
- **Owner**: `TBD`
- **Status**: `Done`

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

- **Date**: `2026-03-31`
- **Ticket**: `N/A (Hilt compile fix)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `@HiltViewModel` classes were accessed from singleton entry points, which bypasses ViewModel lifecycle ownership and breaks Hilt validation.
- **Proposed Action**: Standardize app-screen wiring to resolve `@HiltViewModel` only through Compose/Android ViewModel APIs and pass instances into non-composable screen models.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (compile warning cleanup pass)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Nullability drift left several impossible null checks and one unchecked cast, obscuring intent and increasing warning noise during `:app:compileDebugKotlin`.
- **Proposed Action**: Prefer non-null return contracts in call sites, use safe collection restoration in savers, and update deprecated Material3/WebView usage during warning cleanup slices.
- **Owner**: `TBD`
- **Status**: `Done`

