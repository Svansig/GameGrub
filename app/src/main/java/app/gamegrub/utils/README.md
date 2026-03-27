# Utils Folder

> **Note**: This folder contains 49 files and is a candidate for reorganization. See `REORGANIZATION_PLAN.md` for proposed structure.

This folder contains utility classes used across the application. Due to organic growth, it's a mix of different categories.

## Current Categories

### Container / Wine Management

| File | Purpose |
|------|---------|
| `ContainerUtils.kt` | Wine/Proton container creation, launching, management |
| `ContainerMigrator.kt` | Migrate containers between storage locations |
| `LaunchDependencies.kt` | Pre-launch dependency checking |
| `PreInstallSteps.kt` | Pre-installation steps for containers |

### Steam-Specific

| File | Purpose |
|------|---------|
| `SteamUtils.kt` | Steam-specific utilities (config, login, app detection) |
| `SteamTokenHelper.kt` | Steam token management |
| `SteamTokenLogin.kt` | Steam login token handling |
| `SteamControllerVdfUtils.kt` | Steam controller config parsing |

### Game Management

| File | Purpose |
|------|---------|
| `GameMetadataManager.kt` | Game metadata fetching/caching |
| `GameCompatibilityService.kt` | Compatibility database lookups |
| `GameCompatibilityCache.kt` | Compatibility result caching |
| `GameFeedbackUtils.kt` | User feedback for games |
| `CustomGameScanner.kt` | Scan for custom (non-Steam) games |
| `CustomGameCache.kt` | Custom game data caching |
| `ExecutableSelectionUtils.kt` | Select game executable |
| `GameGridDB.kt` | SteamGridDB integration for game images |

### Authentication

| File | Purpose |
|------|---------|
| `PlatformAuthUtils.kt` | Platform-agnostic auth utilities |
| `PlatformOAuthHandlers.kt` | OAuth flow handlers |
| `PlayIntegrity.kt` | Google Play Integrity checks |
| `KeyAttestationHelper.kt` | Device attestation |
| `AuthUrlRedaction.kt` | Redact sensitive URLs for logging |

### Storage / Files

| File | Purpose |
|------|---------|
| `FileUtils.kt` | General file operations |
| `StorageUtils.kt` | Storage path management |
| `KeyValueUtils.kt` | KeyValue config file handling |
| `ManifestInstaller.kt` | Install from manifests |
| `ManifestRepository.kt` | Manifest data access |
| `ManifestModels.kt` | Manifest data models |
| `ManifestComponentHelper.kt` | Manifest component utils |
| `LicenseSerializer.kt` | License data serialization |

### Network

| File | Purpose |
|------|---------|
| `NetworkUtils.kt` | Network connectivity checks |
| `UpdateChecker.kt` | App update checking |

### Device / Hardware

| File | Purpose |
|------|---------|
| `DeviceUtils.kt` | Device information |
| `HardwareUtils.kt` | Hardware detection |

### UI / Compose

| File | Purpose |
|------|---------|
| `CoilDecoders.kt` | Image decoders for Coil (image loading) |
| `IconSwitcher.kt` | Game icon switching |
| `LocaleHelper.kt` | Localization helpers |
| `PaddingUtils.kt` | Padding utilities |
| `ShortcutUtils.kt` | Home screen shortcuts |
| `SupportersUtils.kt` | Supporter-related UI |

### General Utilities

| File | Purpose |
|------|---------|
| `DateTimeUtils.kt` | Date/time parsing and formatting |
| `StringUtils.kt` | String manipulation |
| `MathUtils.kt` | Math utilities |
| `FlowUtils.kt` | Kotlin Flow extensions |
| `NoToast.kt` | Toast message suppression |
| `IntentLaunchManager.kt` | External app launching |
| `BestConfigService.kt` | Best configuration selection |
| `MarkerUtils.kt` | File marker utilities (install complete, etc.) |
| `UpdateInstaller.kt` | App update installation |

---

## Recommended Reorganization

See `REORGANIZATION_PLAN.md` for the proposed structure:

```
utils/
├── container/       # Wine/Proton container management
├── steam/          # Steam-specific utilities
├── auth/           # Authentication utilities
├── game/           # Game management
├── storage/        # Storage/file utilities
├── manifest/       # Manifest handling
├── network/        # Network utilities
├── device/         # Device utilities
└── general/       # General utilities
```

---

## Adding New Utils

When adding a new utility:

1. **Check existing categories** - Maybe it fits an existing category
2. **Add KDoc** - Explain what the utility does
3. **Consider extraction** - If it's platform-specific, consider moving to that service's folder
4. **Add tests** - Utility classes are easy to test

### Example KDoc:

```kotlin
/**
 * Utility for [specific purpose].
 *
 * @param param Description of parameter
 * @return Description of return value
 * @throws ExceptionWhen This happens when...
 */
fun doSomething(param: String): Result
```
