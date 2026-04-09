# Test Gap Matrix (2026-04-09)

> This matrix maps major features to current test coverage and identifies gaps.

## Coverage Summary

| Feature Area | Test Count | Status |
|---|---|---|
| Steam Service/Domain | ~20 | Good |
| GOG Service | ~3 | Fair |
| Epic Service | ~2 | Fair |
| Amazon Service | ~1 | Poor |
| UI/ViewModel | ~10 | Fair |
| Launch/Telemetry | ~5 | Fair |
| Container/Runtime | ~4 | Fair |
| Data/Storage | ~2 | Poor |
| Utils/Misc | ~3 | Fair |

## Detailed Gap Analysis

### P0 - Critical Gaps (No Tests)

| Feature | Risk | Recommended Tests |
|---|---|---|
| **Authentication (Steam)** | High - Auth regressions cause login failures | OAuth flow, token refresh, session handling |
| **Library Refresh** | High - Stale data, missing games | Sync orchestration, incremental updates |
| **Game Launch (Core)** | Critical - Launch failures block core value | LaunchEngine.execute, session composition |
| **Download/Install** | Critical - Block game play | Download queue, resume, verification |

### P1 - High Priority Gaps

| Feature | Risk | Recommended Tests |
|---|---|---|
| **GOG Auth** | Medium - Login failures | OAuth, API client, token management |
| **Epic Auth** | Medium - Login failures | OAuth, API client, token management |
| **Amazon Auth** | Medium - Login failures | OAuth, API client, token management |
| **Cloud Saves (GOG)** | Medium - Data loss | Sync, conflict resolution |
| **Cloud Saves (Epic)** | Medium - Data loss | Sync, conflict resolution |
| **Container Lifecycle** | High - Launch crashes | Create, destroy, state transitions |
| **Settings Persistence** | Medium - Preference loss | PrefManager, DataStore |

### P2 - Medium Priority Gaps

| Feature | Risk | Recommended Tests |
|---|---|---|
| Game Fixes | Low - Workarounds available | Fix application, compatibility |
| ImageFs/Storage | Low - Legacy migration | Bundle installation |
| PrefManager Edge Cases | Low - Minor UX | Corruption handling, migration |

## Top 10 Missing Tests (by priority)

1. `SteamService` auth flow (token refresh, session recovery)
2. `LaunchEngine` execute path (core launch)
3. `DownloadManager` queue and resume (core feature)
4. `LibraryRefreshOrchestrator` (library data integrity)
5. `GOGService` OAuth + API (account connection)
6. `EpicService` OAuth + API (account connection)
7. `AmazonService` OAuth + API (account connection)
8. `CloudSavesManager` sync (all platforms)
9. `ContainerManager` lifecycle (container state)
10. `SessionAssembler` compose path (launch architecture)

## Test File Inventory (Current)

### Steam Service/Domain (~20 tests)
- SteamInstalledExeManagerTest
- SteamLibraryDomainTest
- SteamDlcDepotManagerTest
- SteamDlcOwnershipManagerTest
- SteamDepotSelectionManagerTest
- SteamDownloadPlanManagerTest
- SteamLaunchConfigManagerTest
- SteamControllerConfigManagerTest
- SteamControllerTemplateRoutingManagerTest
- SteamControllerWorkshopDownloadManagerTest
- SteamDomainManagersSmokeTest
- + others

### Launch/Telemetry (~5 tests)
- LaunchEngineActiveSessionTest
- ActiveSessionStoreTest
- LaunchFingerprintTest
- GameLaunchRetryPolicyTest

### UI/ViewModel (~10 tests)
- LibraryViewModelOwnerFilterTest
- LibraryScreenEmptyStateTest
- MainViewModelPolicyTest
- LibraryAuthResultParserTest
- ImmersiveModeManagerTest
- OrientationManagerTest
- OrientationPolicyFlowTest
- NotificationPermissionGateTest
- StoragePermissionGateTest

### Container/Runtime (~4 tests)
- ContainerManifestTest
- RuntimeManifestTest
- DriverManifestTest

### Services (~8 tests)
- SdCardDetectionTest
- GOGAuthManagerTest
- GOGManifestParserTest
- GOGConstantsTest
- EpicManagerTest
- EpicCloudSavesTest
- AmazonManifestTest
- InstalledGamesStartupValidatorTest

### Data/Utils (~4 tests)
- NetworkManagerTest
- StringUtilsTest
- CompatibilityStatusMapperTest
- LibraryQueryMatcherTest
- + legacy tests

## Follow-up Tickets

| Ticket | Description |
|---|---|
| TEST-002 | Auth/library regression tests (addresses gaps 1, 5, 6, 7) |
| TEST-004 | Launch/resume smoke tests (addresses gaps 2, 8, 9) |
| TEST-013 | Service-domain boundary contract tests (addresses gaps 3, 4) |

---
*Generated: 2026-04-09*
*Source: Test file inventory + feature risk analysis*