# Reorganization Plan

> **Status**: Phases 1 & 2 Complete ✅ | Phase 3 Pending

---

## Phase 1: Service Layer Reorganization ✅ COMPLETE

### Final Structure

```
service/
├── DownloadService.kt         # Shared cross-platform
├── NotificationHelper.kt      # Shared notifications
├── ServiceLifecycleManager.kt
├── steam/                     # ✅ Created
│   ├── SteamService.kt
│   ├── SteamAutoCloud.kt
│   ├── SteamUnifiedFriends.kt
│   ├── AchievementWatcher.kt
│   └── SteamPaths.kt
├── gog/
├── epic/
└── amazon/
```

### Changes Made
| Action | Status |
|--------|--------|
| Created `service/steam/` directory | ✅ |
| Moved 5 Steam files to `service/steam/` | ✅ |
| Updated package declarations | ✅ |
| Updated imports across codebase | ✅ |

---

## Phase 2: Utils Reorganization ✅ COMPLETE

### Final Structure

```
utils/
├── auth/                      # ✅ Authentication utilities
│   ├── AuthUrlRedaction.kt
│   ├── KeyAttestationHelper.kt
│   ├── PlatformAuthUtils.kt
│   ├── PlatformOAuthHandlers.kt
│   └── PlayIntegrity.kt
├── container/                 # ✅ Wine/Proton container management
│   ├── ContainerMigrator.kt
│   ├── ContainerUtils.kt
│   ├── LaunchDependencies.kt
│   ├── PreInstallSteps.kt
│   ├── launchdependencies/
│   │   ├── GogScriptInterpreterDependency.kt
│   │   └── LaunchDependency.kt
│   └── preInstallSteps/
│       ├── GogScriptInterpreterStep.kt
│       ├── OpenALStep.kt
│       ├── PhysXStep.kt
│       ├── PreInstallStep.kt
│       ├── VcRedistStep.kt
│       └── XnaFrameworkStep.kt
├── device/                    # ✅ Device utilities
│   ├── DeviceUtils.kt
│   └── HardwareUtils.kt
├── game/                      # ✅ Game data management
│   ├── BestConfigService.kt
│   ├── CustomGameCache.kt
│   ├── CustomGameScanner.kt
│   ├── ExecutableSelectionUtils.kt
│   ├── ExeIconExtractor.kt
│   ├── GameCompatibilityCache.kt
│   ├── GameCompatibilityService.kt
│   ├── GameFeedbackUtils.kt
│   └── GameMetadataManager.kt
├── general/                   # ✅ General utilities
│   ├── DateTimeUtils.kt
│   ├── FlowUtils.kt
│   ├── IntentLaunchManager.kt
│   ├── MathUtils.kt
│   ├── NoToast.kt
│   ├── StringUtils.kt
│   └── UpdateInstaller.kt
├── manifest/                  # ✅ Manifest handling
│   ├── ManifestComponentHelper.kt
│   ├── ManifestInstaller.kt
│   ├── ManifestModels.kt
│   └── ManifestRepository.kt
├── network/                   # ✅ Network utilities
│   ├── NetworkUtils.kt
│   └── UpdateChecker.kt
├── steam/                     # ✅ Steam-specific utilities
│   ├── KeyValueUtils.kt
│   ├── LicenseSerializer.kt
│   ├── SteamControllerVdfUtils.kt
│   ├── SteamGridDB.kt
│   ├── SteamTokenHelper.kt
│   ├── SteamTokenLogin.kt
│   └── SteamUtils.kt
└── storage/                   # ✅ Storage utilities
    ├── FileUtils.kt
    ├── MarkerUtils.kt
    └── StorageUtils.kt
```

### UI/Util (Moved out of utils)

```
ui/util/                       # ✅ UI-specific utilities
├── CoilDecoders.kt
├── IconSwitcher.kt
├── LocaleHelper.kt
├── PaddingUtils.kt
├── ShortcutUtils.kt
└── SupportersUtils.kt
```

### Changes Made
| Action | Status |
|--------|--------|
| Created 8 new subdirectories | ✅ |
| Moved 43 files to domain-specific folders | ✅ |
| Updated package declarations in all moved files | ✅ |
| Updated imports across 71+ files in codebase | ✅ |
| Verified no remaining old imports | ✅ |

---

## Phase 3: SteamService Decomposition 🔲 PENDING

### Current State
- `SteamService.kt` is ~3800 lines
- Handles: authentication, library sync, cloud saves, achievements, input, friends

### Proposed Decomposition
```
service/steam/
├── SteamService.kt              # Core service (reduced)
├── SteamAuthService.kt          # Authentication & login
├── SteamLibraryManager.kt       # Game library sync
├── SteamCloudSavesManager.kt    # Cloud save management
├── SteamAchievementManager.kt   # Achievement tracking
├── SteamInputManager.kt         # Controller input
└── SteamFriendsManager.kt       # Friends & chat
```

---

## Implementation Priority

| Priority | Task | Status |
|----------|------|--------|
| ~~High~~ | Fix Steam service inconsistency | ✅ Done |
| ~~Medium~~ | Reorganize utils/ into subfolders | ✅ Done |
| Low | Decompose SteamService.kt | 🔲 Pending |
| Low | Add more unit tests | 🔲 Pending |

---

## Benefits Achieved

1. **Discoverability**: Developers can find utilities by domain
2. **Maintainability**: Clear separation of concerns
3. **Scalability**: Easy to add new domain-specific utilities
4. **Consistency**: All services follow same pattern

---

## Next Steps

1. Decompose `SteamService.kt` into focused managers
2. Add unit tests for moved utilities
3. Update documentation/README with new structure
4. Consider adding KDoc comments to public APIs
