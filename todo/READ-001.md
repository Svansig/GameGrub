# READ-001 - Split oversized Kotlin files by responsibility

- **ID**: `READ-001`
- **Area**: `ui + service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Very large files reduce readability, reviewability, and safe iteration speed.

## Scope

- In scope:
  - Inventory oversized files and split candidates.
  - Start with top 3 high-impact files.
- Out of scope:
  - Functional redesign during split.

## Oversized File Priority Matrix (2026-04-03)

Scoring model (1-5 each, higher = more urgent):
- Size (line count)
- Coupling/global-state pressure
- Ticket urgency alignment
- Architecture/doc urgency alignment

| File | Size | Coupling | Ticket urgency | Arch/docs urgency | Total |
|---|---:|---:|---:|---:|---:|
| `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt` | 5 | 5 | 5 | 4 | 19 |
| `app/src/main/java/app/gamegrub/service/steam/SteamService.kt` | 5 | 5 | 5 | 5 | 20 |
| `app/src/main/java/app/gamegrub/ui/component/QuickMenu.kt` | 5 | 3 | 4 | 3 | 15 |
| `app/src/main/java/app/gamegrub/ui/screen/library/appscreen/SteamAppScreen.kt` | 4 | 4 | 5 | 4 | 17 |
| `app/src/main/java/app/gamegrub/ui/screen/xserver/XServerScreen.kt` | 5 | 4 | 3 | 3 | 15 |
| `app/src/main/java/app/gamegrub/service/gog/GOGDownloadManager.kt` | 4 | 4 | 3 | 3 | 14 |
| `app/src/main/java/app/gamegrub/ui/component/dialog/ElementEditorDialog.kt` | 4 | 3 | 3 | 2 | 12 |
| `app/src/main/java/app/gamegrub/service/epic/EpicCloudSavesManager.kt` | 4 | 4 | 3 | 3 | 14 |
| `app/src/main/java/app/gamegrub/ui/component/dialog/ContainerConfigDialog.kt` | 4 | 3 | 3 | 3 | 13 |
| `app/src/main/java/app/gamegrub/utils/container/ContainerUtils.kt` | 4 | 5 | 5 | 5 | 19 |

### Top 3 Immediate Targets

1. `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`
2. `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt`
3. `app/src/main/java/app/gamegrub/utils/container/ContainerUtils.kt`

### Decomposition-Ready Checklist

- [ ] **SteamService track**: execute staged decomposition in `SRV-018` with companion/facade reduction aligned to `ARCH-024` and `docs/steam-service-decomposition-plan.md`.
- [ ] **GameGrubMain track**: execute orchestration extraction steps in `UI-005` so composable stays routing/rendering focused.
- [ ] **ContainerUtils track**: execute ownership migration in `COH-026` using split targets documented in `docs/utils-ownership-audit-2026-04-03.md`.
- [ ] Capture child-ticket split notes in each target ticket before implementation PRs.

## Acceptance Criteria

- [ ] Oversized file list produced with target split plan.
- [ ] At least one high-impact file split without behavior change.
- [ ] New file boundaries follow project conventions.

## Validation

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew lintKotlin`

## Links

- Related docs: `docs/ui-placement/ui-migration-playbook.md`
- PR: `TBD`

