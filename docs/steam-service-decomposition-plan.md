# SteamService Decomposition Plan

## Overview

Transform SteamService from a 3800-line god class with 38 managers into a thin coordinator backed by 5 well-scoped domain classes.

**Current State:**
- SteamService: ~2662 lines
- 38 managers in `managers/`
- 3 aggregator managers (Account, Session, Library) + fragmented Install domain

**Target State:**
- SteamService: ~300 lines (thin coordinator)
- 5 domain classes with clear boundaries
- Managers consolidated into domains

---

## Phase 1: Create Domain Coordinator Classes

### 1.1 SteamAccountDomain
**New file:** `service/steam/domain/SteamAccountDomain.kt`

```kotlin
@Singleton
class SteamAccountDomain @Inject constructor(
    val authService: SteamAuthService,
    val userManager: SteamUserManager,
    val friendsManager: SteamFriendsManager,
    val deviceIdentityManager: SteamDeviceIdentityManager,
)
```

**Purpose:** Wraps `SteamAccountManager`. Exists for symmetry with other domains.

**Note:** This is already mostly done—`SteamAccountManager` already aggregates these 4. Just rename and relocate.

---

### 1.2 SteamLibraryDomain
**New file:** `service/steam/domain/SteamLibraryDomain.kt`

```kotlin
@Singleton
class SteamLibraryDomain @Inject constructor(
    private val libraryClient: SteamLibraryClient,
    private val connection: SteamConnection,
    // DAOs
    private val appDao: SteamAppDao,
    private val licenseDao: SteamLicenseDao,
    private val appInfoDao: AppInfoDao,
    private val cachedLicenseDao: CachedLicenseDao,
    private val downloadingAppInfoDao: DownloadingAppInfoDao,
    // Managers to delegate to
    private val picsChangesManager: PicsChangesManager,
    private val downloadManager: DownloadManager,
    private val catalogManager: SteamCatalogManager,
)
```

**Purpose:** Merges:
- `SteamLibraryManager` (DB + library sync)
- `PicsChangesManager` (PICS change tracking)
- `DownloadManager` (download state)
- `SteamCatalogManager` (depot/app metadata)

**Rationale:** These are all "library data" concerns. PICS changes update library metadata. Download state is tied to library entries. Catalog is cached library data.

---

### 1.3 SteamSessionDomain
**New file:** `service/steam/domain/SteamSessionDomain.kt`

```kotlin
@Singleton
class SteamSessionDomain @Inject constructor(
    val appSessionManager: SteamAppSessionManager,
    val sessionFilesManager: SteamSessionFilesManager,
    val ticketManager: SteamTicketManager,
)
```

**Purpose:** Wraps `SteamSessionManager`. Already well-structured—just rename for symmetry.

---

### 1.4 SteamCloudStatsDomain
**New file:** `service/steam/domain/SteamCloudStatsDomain.kt`

```kotlin
@Singleton
class SteamCloudStatsDomain @Inject constructor(
    private val cloudClient: SteamCloudClient,
    private val statsClient: SteamStatsClient,
    private val appInfoDao: AppInfoDao,
) {
    // Internal managers - create as inner classes or private
    private val cloudSavesManager: CloudSavesManager = ...
    private val achievementManager: AchievementManager = ...
}
```

**Purpose:** Merges:
- `SteamCloudSavesManager`
- `SteamAchievementManager`

**Rationale:** Both are "gameplay session bookends"—what happens before launch (cloud download) and after exit (cloud upload + achievement sync).

---

### 1.5 SteamInstallDomain (THE BIG ONE)
**New file:** `service/steam/domain/SteamInstallDomain.kt`

```kotlin
@Singleton
class SteamInstallDomain @Inject constructor(
    private val libraryClient: SteamLibraryClient,
    private val connection: SteamConnection,
) {
    // Install orchestration
    fun buildDownloadPlan(...): DownloadPlan = ...
    fun resolveInstalledExe(...): String = ...
    fun getDownloadableDepots(...): Map<Int, DepotInfo> = ...
    
    // Input handling (merged from 4 managers)
    fun resolveControllerConfig(...): String? = ...
    fun downloadWorkshopConfig(...): Deferred<Unit> = ...
    
    // Depot operations (merged from 4 managers)
    fun selectDepots(...): Map<Int, DepotInfo> = ...
    fun checkDlcOwnership(...): Set<Int> = ...
    fun resolveMainAppDlcIds(...): List<Int> = ...
}
```

**Purpose:** Consolidates the fragmented Install domain:
- `SteamInstallManager` (just delegates)
- `SteamDownloadPlanManager`
- `SteamDepotSelectionManager`
- `SteamDlcDepotManager`
- `SteamDlcOwnershipManager`
- `SteamInstalledExeManager`
- `SteamLaunchConfigManager`
- `SteamInputManager`
- `SteamControllerConfigManager`
- `SteamControllerTemplateRoutingManager`
- `SteamControllerWorkshopDownloadManager`

**Rationale:** These are all "install pipeline" concerns—how a game gets from Steam server to playable on device.

---

## Phase 2: Move Manager Logic Into Domains

### 2.1 AccountDomain (Simplest)
**Actions:**
1. Copy `SteamAccountManager.kt` → `domain/SteamAccountDomain.kt`
2. Rename class to `SteamAccountDomain`
3. Update imports
4. Delete old file

**No logic changes needed**—just rename and move.

---

### 2.2 LibraryDomain (Medium complexity)
**Actions:**
1. Move `SteamLibraryManager` logic into domain
2. Move `PicsChangesManager` logic into domain (or keep as delegate)
3. Move `DownloadManager` logic into domain
4. Move `SteamCatalogManager` logic into domain
5. Update DI bindings

**Key decisions:**
- Keep managers as internal classes? Or inline all logic?
- Recommendation: Keep as private inner classes for now (easier to test incrementally)

---

### 2.3 SessionDomain (Simplest)
**Actions:**
1. Copy `SteamSessionManager.kt` → `domain/SteamSessionDomain.kt`
2. Rename class
3. Update dependencies from injected to constructor

---

### 2.4 CloudStatsDomain (Medium complexity)
**Actions:**
1. Move `SteamCloudSavesManager` logic into domain
2. Move `SteamAchievementManager` logic into domain
3. Delete `SteamCloudStatsManager` (it just aggregates these two)

**Key insight:** `SteamCloudStatsManager` exists only to aggregate. Delete it and put logic directly in domain.

---

### 2.5 InstallDomain (Complex)
**Actions:**
1. This is the big consolidation. Strategy:
   - Create the domain class with all the public APIs
   - Move logic from each manager into domain methods
   - Delete the now-redundant managers

**Order of operations:**
1. Create domain with all 10+ manager references injected
2. For each manager, create a corresponding method in domain that delegates to it
3. Run tests to verify nothing broke
4. Inline the manager logic (remove the indirection)
5. Delete the manager class

**Files to delete:**
- `SteamInstallManager.kt`
- `SteamDownloadPlanManager.kt`
- `SteamDepotSelectionManager.kt`
- `SteamDlcDepotManager.kt`
- `SteamDlcOwnershipManager.kt`
- `SteamInstalledExeManager.kt`
- `SteamLaunchConfigManager.kt`
- `SteamInputManager.kt`
- `SteamControllerConfigManager.kt`
- `SteamControllerTemplateRoutingManager.kt`
- `SteamControllerWorkshopDownloadManager.kt`

---

## Phase 3: Update SteamService to Use Domains

### Before
```kotlin
class SteamService : Service() {
    @Inject lateinit var libraryManager: SteamLibraryManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var picsChangesManager: PicsChangesManager
    @Inject lateinit var sessionManager: SteamSessionManager
    @Inject lateinit var accountManager: SteamAccountManager
    @Inject lateinit var cloudStatsManager: SteamCloudStatsManager
    // ... 30+ more managers
}
```

### After
```kotlin
class SteamService : Service() {
    @Inject lateinit var accountDomain: SteamAccountDomain
    @Inject lateinit var libraryDomain: SteamLibraryDomain
    @Inject lateinit var sessionDomain: SteamSessionDomain
    @Inject lateinit var cloudStatsDomain: SteamCloudStatsDomain
    @Inject lateinit var installDomain: SteamInstallDomain
    
    // Keep only service-level concerns:
    // - Connection management (steamClient, callbackManager)
    // - Foreground service lifecycle
    // - Network monitoring
    // - PICS channels and jobs
}
```

**Update all references in SteamService:**
1. `libraryManager.xxx()` → `libraryDomain.xxx()`
2. `downloadManager.xxx()` → `libraryDomain.downloadManager.xxx()`
3. `picsChangesManager.xxx()` → `libraryDomain.picsChangesManager.xxx()`
4. `sessionManager.xxx()` → `sessionDomain.xxx()`
5. `accountManager.xxx()` → `accountDomain.xxx()`
6. `cloudStatsManager.xxx()` → `cloudStatsDomain.xxx()`
7. `installManager.xxx()` → `installDomain.xxx()`
8. `catalogManager.xxx()` → `libraryDomain.catalogManager.xxx()`
9. `inputManager.xxx()` → `installDomain.inputManager.xxx()`

---

## Phase 4: Delete Empty Manager Classes

### After logic moves, delete:
```
service/steam/managers/
├── SteamAccountManager.kt      (moved to domain)
├── SteamSessionManager.kt      (moved to domain)
├── SteamLibraryManager.kt      (moved to domain)
├── SteamCloudStatsManager.kt   (logic inlined, delete)
├── SteamInstallManager.kt      (moved to domain)
├── SteamInputManager.kt        (moved to domain)
├── SteamControllerConfigManager.kt       (deleted)
├── SteamControllerTemplateRoutingManager.kt (deleted)
├── SteamControllerWorkshopDownloadManager.kt (deleted)
├── SteamDownloadPlanManager.kt (deleted)
├── SteamDepotSelectionManager.kt (deleted)
├── SteamDlcDepotManager.kt     (deleted)
├── SteamDlcOwnershipManager.kt (deleted)
├── SteamInstalledExeManager.kt (deleted)
├── SteamLaunchConfigManager.kt (deleted)
└── SteamCatalogManager.kt      (moved to domain)
```

### Keep (reusable components, not managers):
```
service/steam/managers/
├── SteamAuthService.kt         (domain delegates to this)
├── SteamUserManager.kt         (domain delegates to this)
├── SteamFriendsManager.kt     (domain delegates to this)
├── SteamDeviceIdentityManager.kt (domain delegates to this)
├── SteamAppSessionManager.kt   (session domain delegates)
├── SteamSessionFilesManager.kt (session domain delegates)
├── SteamTicketManager.kt       (session domain delegates)
├── SteamCloudSavesManager.kt  (cloudstats domain delegates)
├── SteamAchievementManager.kt (cloudstats domain delegates)
├── PicsChangesManager.kt      (library domain delegates)
├── DownloadManager.kt          (library domain delegates)
└── SteamUnifiedFriends.kt     (separate, used by events)
```

---

## Phase 5: Shrink SteamService to Thin Coordinator

### Target: ~300 lines

**Keep in SteamService:**
1. Service lifecycle (`onCreate`, `onStartCommand`, `onDestroy`)
2. Connection management (`steamClient`, `callbackManager`, `callbackSubscriptions`)
3. Steam handler lazy getters (`steamUser`, `steamApps`, `steamFriends`, etc.)
4. Network monitoring (`connectivityManager`, `networkCallback`)
5. PICS channels and jobs (`appPicsChannel`, `packagePicsChannel`, `picsGetProductInfoJob`, etc.)
6. Foreground service setup (`notificationHelper`)
7. Event emission (`GameGrubApp.events.emit(...)`)

**Move to domains:**
1. All business logic (library queries, download planning, session orchestration)
2. DB operations
3. Steam API calls

### Extract Further: Consider SteamPicsSyncService

The continuous PICS polling (`continuousPICSGetProductInfo`, `continuousPICSChangesChecker`) could be its own service class, started/stopped by SteamService but running independently.

```kotlin
class SteamPicsSyncService(
    private val libraryDomain: SteamLibraryDomain,
    private val steamClient: SteamClient,
) {
    // Runs PICS sync jobs
    // Emits events on changes
}
```

---

## File Movement Summary

### New Files (Phase 1)
```
service/steam/domain/
├── SteamAccountDomain.kt
├── SteamLibraryDomain.kt
├── SteamSessionDomain.kt
├── SteamCloudStatsDomain.kt
└── SteamInstallDomain.kt
```

### Modified Files (Phase 2-3)
- `SteamService.kt` — Update injections and method calls
- `SteamModule.kt` — Add domain bindings

### Deleted Files (Phase 4)
- All manager files that get inlined into domains
- Specifically: 20+ files from the managers folder

---

## Testing Strategy

### Before Each Phase
1. Run `./gradlew testDebugUnitTest` — ensure existing tests pass

### After Each Phase
1. Run `./gradlew testDebugUnitTest`
2. Run `./gradlew assembleDebug` — verify build
3. Manual smoke test on device

### Key Test Files to Update
- `SteamDomainManagersSmokeTest.kt` — update to test domains instead of managers

---

## Risks and Mitigations

| Risk | Mitigation |
|------|-------------|
| Breaking existing API (static methods on SteamService) | Keep static methods on SteamService, delegate to domains internally |
| Circular dependencies | Domains depend on interfaces (SteamConnection, SteamLibraryClient), not each other |
| Test breakage | Keep existing manager classes as delegates initially, migrate tests incrementally |
| Runtime crashes | Test thoroughly at each phase before deleting manager classes |

---

## Timeline Estimate

| Phase | Effort | Files Changed |
|-------|--------|---------------|
| Phase 1: Create domains | Low | 5 new files |
| Phase 2: Move logic | Medium | 20+ files modified |
| Phase 3: Update SteamService | Medium | 1 file (SteamService.kt) + DI |
| Phase 4: Delete empty managers | Low | 20+ deletions |
| Phase 5: Shrink SteamService | Low | 1 file (SteamService.kt) |

**Total:** ~30 file changes over 5 phases, can be done incrementally.