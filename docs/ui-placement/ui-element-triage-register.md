# UI Element Triage Register

This register tracks concrete UI elements that appear misplaced, over-coupled, dead, or inconsistent.

## Triage Criteria

An element is considered "doesn't belong" when one or more conditions apply:

- `Boundary`: UI code performs service/business/container orchestration that should be in ViewModel/domain.
- `Lifecycle`: UI creates unmanaged scopes/jobs that can outlive UI lifecycle.
- `Structure`: package/folder placement conflicts with current project conventions.
- `Dead or legacy`: UI assets/components are obsolete, duplicated, or disconnected from active flow.
- `Docs drift`: architecture/developer docs point to outdated locations.

## Active Inventory

| ID | Priority | Type | Location | Finding | Proposed Direction | Ticket | Status |
|---|---|---|---|---|---|---|---|
| UI-001 | P1 | Boundary | `app/src/main/java/app/gamegrub/ui/screen/library/appscreen/GOGAppScreen.kt` | UI class owns download/install/uninstall operations and direct service calls. | Move operational logic into a platform ViewModel/use-case layer; keep composable-facing callbacks only. | `todo/UI-001.md` | Backlog |
| UI-002 | P1 | Lifecycle | `app/src/main/java/app/gamegrub/ui/screen/library/appscreen/GOGAppScreen.kt` | Uses `CoroutineScope(Dispatchers.IO).launch` from UI class in multiple paths. | Replace with lifecycle-aware scope from ViewModel (`viewModelScope`) and UI event dispatch. | `todo/UI-002.md` | Backlog |
| UI-003 | P1 | Lifecycle | `app/src/main/java/app/gamegrub/ui/screen/library/appscreen/SteamAppScreen.kt` | Similar unmanaged IO scopes for download/update actions. | Route actions through ViewModel intents/state reducers. | `todo/UI-003.md` | Backlog |
| UI-004 | P1 | Boundary | `app/src/main/java/app/gamegrub/ui/screen/library/LibraryScreen.kt` | OAuth + credential checks happen directly in composable and directly call handlers/services. | Move auth workflow and platform status into ViewModel state. | `todo/UI-004.md` | Backlog |
| UI-005 | P1 | Boundary | `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt` | Entry composable handles broad orchestration (service starts, launch resolution, launch prep, cloud sync branching). | Split orchestration into dedicated coordinator/use-case + ViewModel events; keep composable focused on routing and rendering. | `todo/UI-005.md` | Backlog |
| UI-006 | P2 | Tooling | `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt` | Ktlint issues (`kdoc` in block, long line) in central file. | Do a focused lint cleanup PR before/alongside refactors. | `todo/UI-006.md` | Backlog |
| UI-007 | P2 | Docs drift | `ARCHITECTURE.md` | UI tree lists `ui/components` and `ui/util`, but active dirs are `ui/component` and `ui/utils`. | Update architecture docs to current paths and reference this package. | `todo/UI-007.md` | Backlog |
| UI-008 | P3 | Placement | `app/src/main/java/app/gamegrub/ui/internal/FakeData.kt` | Preview fake data in `main` with runtime guard. | Consider moving to preview-only or test source set; keep only necessary shared stubs in `main`. | `todo/UI-008.md` | Backlog |
| UI-009 | P3 | Legacy seam | `app/src/main/res/layout/*.xml` + `app/src/main/java/com/winlator/**` | Legacy XML/UI widgets remain in Winlator stack while app UI is Compose-first. | Track as explicit exception boundary; avoid introducing new app-layer UI features in legacy stack. | `todo/UI-009.md` | Backlog |

## Quick Pick Order

Recommended near-term sequence:

1. `UI-006` (lint cleanup in `GameGrubMain`) - reduces noise for larger diffs.
2. `UI-004` (Library auth flow extraction) - high value, moderate size.
3. `UI-001` + `UI-002` (GOG app screen boundary/lifecycle fixes).
4. `UI-003` (Steam app screen lifecycle fix pattern reuse).
5. `UI-007` (docs drift alignment).

## Per-Item Notes Template

Use this for new entries:

```md
### UI-XXX
- **Owner**:
- **Area**:
- **Risk**:
- **User Impact**:
- **Before**:
- **After**:
- **Validation Evidence**:
- **Linked PR/Issue**:
```
