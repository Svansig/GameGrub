# UI Migration Playbook

This playbook defines how to move misplaced UI responsibilities safely and incrementally.

## Migration Principles

- Preserve behavior first, then improve architecture.
- Prefer small PRs scoped to one UI area or one responsibility type.
- Keep old/new pathways from coexisting longer than one iteration.

## Standard Migration Flow

1. Pick one `UI-XXX` item from `ui-element-triage-register.md`.
2. Write a small "before -> after" note in the PR description.
3. Add/adjust ViewModel intent/state for the moved behavior.
4. Move side-effecting calls from composable/UI helper into ViewModel or coordinator.
5. Keep UI callback signatures stable where possible to limit blast radius.
6. Run targeted tests and lint checks.
7. Capture validation evidence using `ui-validation-checklist.md`.

## Playbook by Problem Type

### A) UI directly calling service methods

Example pattern:
- `onClick` in screen/component directly calls `SteamService`/`GOGService`/`EpicService`/`AmazonService`.

Refactor pattern:
- Introduce `UiIntent` (`OnDownloadClick`, `OnLoginRequested`, etc.).
- Handle in ViewModel; call service in `viewModelScope`.
- Emit UI effects/state updates (`snackbarMessage`, `dialogState`, progress values).

### B) UI creating unmanaged coroutine scopes

Example pattern:
- `CoroutineScope(Dispatchers.IO).launch { ... }` in UI classes.

Refactor pattern:
- Replace with ViewModel action handlers and `viewModelScope`.
- Use structured cancellation tied to screen lifecycle.

### C) Large entry composable orchestration

Example pattern:
- `GameGrubMain` contains launch checks, dependency prep, session decisions.

Refactor pattern:
- Extract launch/session orchestration into dedicated coordinator/use-case classes.
- Keep composable focused on nav, dialog rendering, and event bridging.

## Suggested PR Sequence

1. **PR-1: Lint-only cleanup in `GameGrubMain`**
   - Remove invalid KDoc-in-block comments, wrap long logs, no behavior change.
2. **PR-2: Library auth flow extraction**
   - Move OAuth result handling and login-state checks into ViewModel.
3. **PR-3: GOG app screen side-effect migration**
   - Replace direct IO scopes/service operations with ViewModel intents.
4. **PR-4: Steam app screen side-effect migration**
   - Reuse pattern from PR-3.
5. **PR-5: Main launch orchestration decomposition (phase 1)**
   - Extract pure helpers/coordinators while keeping external behavior stable.
6. **PR-6: Documentation alignment**
   - Update architecture docs to canonical paths and reference this package.

## Risk Controls

- Keep each migration behind equivalent UI state transitions to avoid visual regressions.
- Preserve message strings and dialog copy unless intentionally changed.
- For launch flows, verify external-intent paths and pending-launch logic explicitly.

## Definition of Done (Per Item)

- Direct service side effects removed from target UI file (or documented exception).
- No unmanaged coroutine scope remains in target UI file.
- Ktlint and compile checks pass for touched files.
- Validation checklist completed with evidence in PR.

