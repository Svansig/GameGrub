# SteamService Decomposition Plan (Status + Handoff)

## Purpose

This document is a live status tracker for the Steam decomposition effort.
It replaces stale assumptions with current evidence and provides an explicit handoff checklist for the next agent.

Snapshot date: **2026-03-31** (workspace state).

---

## Current Architecture Snapshot (Evidence)

### SteamService

- `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`
  - Still large: **2663 lines**.
  - Injects 5 domains (`SteamAccountDomain`, `SteamLibraryDomain`, `SteamSessionDomain`, `SteamCloudStatsDomain`, `SteamInstallDomain`).
  - Still injects multiple DAOs directly (`SteamAppDao`, `SteamLicenseDao`, `CachedLicenseDao`, etc.).
  - Companion object still provides many static APIs and still routes some calls through manager singletons.

### Domain classes (all exist)

- `app/src/main/java/app/gamegrub/service/steam/domain/SteamAccountDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/domain/SteamLibraryDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/domain/SteamSessionDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/domain/SteamCloudStatsDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/domain/SteamInstallDomain.kt`

### Manager inventory (current)

- Managers folder currently contains **23 files**:
  - `DownloadManager.kt`, `PicsChangesManager.kt`, `SteamAchievementManager.kt`, `SteamAppSessionManager.kt`, `SteamAuthService.kt`, `SteamCatalogManager.kt`, `SteamCloudSavesManager.kt`, `SteamControllerConfigManager.kt`, `SteamControllerTemplateRoutingManager.kt`, `SteamControllerWorkshopDownloadManager.kt`, `SteamDepotSelectionManager.kt`, `SteamDeviceIdentityManager.kt`, `SteamDlcDepotManager.kt`, `SteamDlcOwnershipManager.kt`, `SteamDownloadPlanManager.kt`, `SteamFriendsManager.kt`, `SteamInputManager.kt`, `SteamInstalledExeManager.kt`, `SteamInstallManager.kt`, `SteamLaunchConfigManager.kt`, `SteamSessionFilesManager.kt`, `SteamTicketManager.kt`, `SteamUserManager.kt`.

### Aggregator managers removed

These no longer exist as classes:

- `SteamAccountManager`
- `SteamSessionManager`
- `SteamLibraryManager`
- `SteamCloudStatsManager`

---

## Phase-by-Phase Status Against Original Plan

Status labels:

- `Done`
- `Partial`
- `Not Started`

### Phase 1 - Create domain coordinator classes

- **Status: `Done`**
- All 5 domain files exist under `service/steam/domain/`.

### Phase 2 - Move manager logic into domains

- **Status: `Partial`**
- Done:
  - Domain wrappers exist for account/session/cloud/install concerns.
  - `SteamLibraryDomain` already holds meaningful logic (license sync, owned games fetch, DAO coordination).
- Not done:
  - Most domains still expose manager internals instead of owning behavior.
  - `SteamInstallDomain` is still a thin wrapper over `SteamInstallManager` and `SteamInputManager`.
  - Install sub-managers remain separate (`SteamDownloadPlanManager`, `SteamDepotSelectionManager`, etc.).

### Phase 3 - Update SteamService to use domains

- **Status: `Partial`**
- Done:
  - Domain injection is in place.
  - Several call sites already delegate via domains.
- Not done:
  - Companion still holds manager gateways (`catalogManager`, `installManager`, `inputManager`).
  - Service still calls manager internals via domain getters (`libraryDomain.getDownloadManager()`, `libraryDomain.getPicsChangesManager()`).
  - Cloud/achievement APIs still access `cloudStatsDomain.achievementManager` directly.

### Phase 4 - Delete empty manager classes

- **Status: `Partial`**
- Done:
  - Old high-level aggregator manager classes were removed (account/session/library/cloudstats managers).
- Not done:
  - Install stack managers and several helper managers are still present and actively used.

### Phase 5 - Shrink SteamService to thin coordinator

- **Status: `Not Started`**
- Service remains 2663 lines with substantial business logic and direct DB/network orchestration.

---

## Gap List (Concrete)

### Gap A - Companion object still manager-centric

In `SteamService` companion object:

- `catalogManager` still resolves to `SteamCatalogManager` singleton fallback.
- `installManager` still points to `SteamInstallManager` singleton.
- `inputManager` still points to `SteamInputManager` singleton fallback.

Impact:

- Preserves old architecture shape and keeps static manager coupling.

### Gap B - LibraryDomain still leaks internal managers

`SteamLibraryDomain` exposes:

- `getDownloadManager()`
- `getPicsChangesManager()`
- `getCatalogManager()`

`SteamService` uses these getters for operations such as clear/delete and download cleanup.

Impact:

- Domain is not yet a true boundary; caller still reaches manager-level APIs.

### Gap C - InstallDomain does not own install behavior yet

`SteamInstallDomain` mostly delegates to manager objects and still references manager model types.

Impact:

- The highest-churn and highest-complexity domain remains fragmented.

### Gap D - CloudStatsDomain still exposes manager internals

`SteamService` calls `cloudStatsDomain.achievementManager` directly for generate/clear/sync/store flows.

Impact:

- Domain boundary is bypassed, increasing coupling and migration risk.

### Gap E - SteamService still contains heavy business logic

Examples still in service:

- Download planning and orchestration.
- Steam Input template routing/download glue.
- PICS batching and DB-updating flow coordination.
- Large static API surface in companion object.

Impact:

- Prevents reaching thin-coordinator target and keeps test surface broad.

---

## Next Agent Checklist (Explicit Continuation Plan)

Use this checklist in order. Each step is intentionally small and reviewable.

### 1) Replace manager gateways in SteamService companion

- [ ] Remove companion `catalogManager` property and route usages to domain-owned methods.
- [ ] Remove companion `installManager` property and route usages to `installDomain` methods.
- [ ] Remove companion `inputManager` property and route usages to `installDomain` methods.
- [ ] Keep static API signatures stable where possible to avoid call-site breakage.

Primary file:

- `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`

### 2) Stop exposing manager getters from SteamLibraryDomain

- [ ] Add explicit domain methods for currently leaked operations:
  - `clearPicsChanges()`
  - `clearDownloadState()`
  - `deleteDownloadStateForApp(appId: Int)`
  - `getAllDownloadingApps()`
  - `deleteDownloadingApp(appId: Int)`
- [ ] Replace all `libraryDomain.getDownloadManager()` and `libraryDomain.getPicsChangesManager()` calls in `SteamService`.
- [ ] Remove `getDownloadManager()`, `getPicsChangesManager()`, and `getCatalogManager()` from `SteamLibraryDomain`.

Primary files:

- `app/src/main/java/app/gamegrub/service/steam/domain/SteamLibraryDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`

### 3) Move install/input orchestration into SteamInstallDomain

- [ ] Expand `SteamInstallDomain` API so `SteamService` no longer calls manager singletons directly.
- [ ] Migrate these operations first (highest payoff):
  - build download plan
  - resolve installed exe
  - steam input route/manifest/workshop config workflow
- [ ] Keep behavior unchanged initially; use delegation while moving call sites, then inline manager logic incrementally.

Primary files:

- `app/src/main/java/app/gamegrub/service/steam/domain/SteamInstallDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`
- `app/src/main/java/app/gamegrub/service/steam/managers/SteamInstallManager.kt`
- `app/src/main/java/app/gamegrub/service/steam/managers/SteamInputManager.kt`

### 4) Encapsulate cloud achievement operations in domain

- [ ] Add domain-level methods in `SteamCloudStatsDomain` for generate/clear/getDirs/sync/store.
- [ ] Replace all direct `cloudStatsDomain.achievementManager` accesses in `SteamService`.

Primary files:

- `app/src/main/java/app/gamegrub/service/steam/domain/SteamCloudStatsDomain.kt`
- `app/src/main/java/app/gamegrub/service/steam/SteamService.kt`

### 5) Delete or demote redundant manager layers

- [ ] After call-sites are migrated, remove wrapper-only managers first:
  - `SteamInstallManager.kt`
  - `SteamInputManager.kt`
  - then evaluate `SteamCatalogManager.kt`
- [ ] Keep low-level reusable helpers only if they provide isolated, testable logic.

Primary folder:

- `app/src/main/java/app/gamegrub/service/steam/managers/`

### 6) Reduce SteamService size and responsibilities

- [ ] Move non-lifecycle business logic behind domain methods.
- [ ] Keep in service only lifecycle, connection state, and event wiring.
- [ ] Re-evaluate whether PICS loops should become a dedicated coordinator/service after domain boundaries are stable.

---

## Test and Validation Checklist

Run after each logical step (or grouped PR-sized change):

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Target tests to keep aligned with refactors:

- `app/src/test/java/app/gamegrub/service/steam/managers/SteamDomainManagersSmokeTest.kt`
- `app/src/test/java/app/gamegrub/service/steam/managers/SteamDownloadPlanManagerTest.kt`
- `app/src/test/java/app/gamegrub/service/steam/managers/SteamControllerConfigManagerTest.kt`
- `app/src/test/java/app/gamegrub/service/steam/managers/SteamControllerTemplateRoutingManagerTest.kt`
- `app/src/test/java/app/gamegrub/service/steam/managers/SteamControllerWorkshopDownloadManagerTest.kt`

Important note:

- As domains absorb logic, add domain-focused tests and reduce direct manager-focused tests accordingly.

---

## Recommended Work Split for Parallel Agents

If running multiple agents/PRs in parallel, use this split:

1. **Agent A:** SteamService companion cleanup + library-domain API tightening.
2. **Agent B:** Install domain expansion + install/input call-site migration.
3. **Agent C:** Cloud stats domain encapsulation + tests.

Merge order recommendation: **A -> B -> C** (A reduces shared surface first).

---

## Definition of Done (Decomposition)

The decomposition is complete when all are true:

- `SteamService` has no direct dependency on manager singletons for business behavior.
- Domains expose behavior-oriented APIs, not manager getters.
- Wrapper-only managers are removed or converted to private/internal helpers behind domains.
- Steam service logic is primarily lifecycle/connection/event coordination.
- Tests primarily target domain behavior and key integration seams.
