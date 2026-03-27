# Reorganization Plan

> **Status**: Not yet implemented. Requires LSP tooling for safe refactoring.

## Service Layer Reorganization

### Current Structure (Inconsistent)

```
service/
├── SteamService.kt        # Steam - in root (INCONSISTENT)
├── SteamAutoCloud.kt
├── SteamUnifiedFriends.kt
├── AchievementWatcher.kt
├── DownloadService.kt
├── NotificationHelper.kt
├── gog/                   # GOG - proper subfolder ✓
├── epic/                  # Epic - proper subfolder ✓
└── amazon/                # Amazon - proper subfolder ✓
```

### Proposed Structure

```
service/
├── DownloadService.kt     # Shared cross-platform
├── NotificationHelper.kt  # Shared notifications
├── steam/                 # NEW - consistent with others
│   ├── SteamService.kt
│   ├── SteamAutoCloud.kt
│   ├── SteamUnifiedFriends.kt
│   └── AchievementWatcher.kt
├── gog/
├── epic/
└── amazon/
```

### Changes Required

| Action | Files Affected |
|--------|---------------|
| Create `service/steam/` directory | - |
| Move 4 files to `service/steam/` | Steam files |
| Update package declarations | 4 files moved |
| Update imports | 38 files import Steam |

### Risks
- Without LSP tooling, manual import updates error-prone
- Test files may break
- Could cause build failures

---

## Utils Reorganization Plan

### Current State
57 files in a flat `utils/` folder - difficult to navigate.

### Proposed Structure

```
utils/
├── container/           # Wine/Proton container management
│   ├── ContainerUtils.kt
│   ├── ContainerMigrator.kt
│   ├── LaunchDependencies.kt
│   ├── preInstallSteps/  (already nested)
│   └── launchdependencies/ (already nested)
├── steam/               # Steam-specific utilities
│   ├── SteamUtils.kt
│   ├── SteamTokenHelper.kt
│   ├── SteamTokenLogin.kt
│   └── SteamControllerVdfUtils.kt
├── auth/                # Authentication utilities
│   ├── PlatformAuthUtils.kt
│   ├── PlatformOAuthHandlers.kt
│   ├── PlayIntegrity.kt
│   └── KeyAttestationHelper.kt
├── game/                # Game data management
│   ├── GameMetadataManager.kt
│   ├── GameCompatibilityService.kt
│   ├── GameCompatibilityCache.kt
│   ├── GameFeedbackUtils.kt
│   ├── CustomGameScanner.kt
│   └── CustomGameCache.kt
├── storage/             # Storage utilities
│   ├── FileUtils.kt
│   ├── StorageUtils.kt
│   └── KeyValueUtils.kt
├── manifest/            # Manifest handling
│   ├── ManifestInstaller.kt
│   ├── ManifestRepository.kt
│   ├── ManifestModels.kt
│   └── ManifestComponentHelper.kt
├── network/             # Network utilities
│   ├── NetworkUtils.kt
│   └── UpdateChecker.kt
├── device/              # Device utilities
│   ├── DeviceUtils.kt
│   └── HardwareUtils.kt
├── image/               # Image handling
│   ├── IconSwitcher.kt
│   ├── ExeIconExtractor.kt
│   └── CoilDecoders.kt
└── general/             # General utilities
    ├── DateTimeUtils.kt
    ├── StringUtils.kt
    ├── MathUtils.kt
    ├── FlowUtils.kt
    ├── LocaleHelper.kt
    ├── SupportersUtils.kt
    ├── MarkerUtils.kt
    ├── PaddingUtils.kt
    ├── NoToast.kt
    ├── UpdateInstaller.kt
    ├── LicenseSerializer.kt
    ├── SteamGridDB.kt
    ├── IntentLaunchManager.kt
    ├── AuthUrlRedaction.kt
    └── BestConfigService.kt
```

### Migration Strategy

1. **Phase 1**: Create new subdirectories
2. **Phase 2**: Move files one category at a time
3. **Phase 3**: Update imports (requires LSP)
4. **Phase 4**: Verify build passes

### Notes
- `preInstallSteps/` and `launchdependencies/` already nested - can keep or move to `container/`
- Some files like `ShortcutUtils.kt`, `SteamGridDB.kt` need categorization decisions

---

## Implementation Priority

1. **High**: Fix Steam service inconsistency (create `steam/` subfolder)
2. **Medium**: Reorganize utils/ into subfolders
3. **Low**: Consider breaking up `SteamService.kt` (3800 lines) into smaller services

---

## Tools Needed

To safely implement these changes:
- Kotlin LSP server for automated refactoring
- Or Android Studio's built-in refactoring tools
- Always run `./gradlew assembleDebug` after changes
- Run `./gradlew test` to verify tests pass
