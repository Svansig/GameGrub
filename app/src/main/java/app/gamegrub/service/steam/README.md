# Steam Service

Steam platform integration for GameNative.

## Architecture

```
SteamService (Main Service - 3800+ lines)
    │
    ├── SteamAutoCloud       → Automatic cloud save sync
    ├── SteamUnifiedFriends  → Friends list management
    └── AchievementWatcher   → Achievement monitoring

Shared Services:
    ├── DownloadService      → Cross-platform download queue
    └── NotificationHelper   → Centralized notifications
```

## Files

| File                     | Purpose                                   |
|--------------------------|-------------------------------------------|
| `SteamService.kt`        | Main Android foreground service (massive) |
| `SteamAutoCloud.kt`      | Cloud save synchronization                |
| `SteamUnifiedFriends.kt` | Friends list integration                  |
| `AchievementWatcher.kt`  | Steam achievement monitoring              |
| `DownloadService.kt`     | Shared download queue                     |
| `NotificationHelper.kt`  | Notification management                   |

## Database

- **Entities**:
    - `SteamApp` - Installed games
    - `SteamLicense` - Game licenses
    - `AppInfo` - App metadata
    - `CachedLicense` - Cached license data
- **DAOs**: `SteamAppDao`, `SteamLicenseDao`, `AppInfoDao`, etc.

## Key Flows

### Authentication

- Uses JavaSteam library for Steam authentication
- Supports QR code login
- Credentials stored securely

### Library Sync

1. `SteamService.start()` connects to Steam network
2. Fetches owned games via Steam API
3. Stores in Room database

### Game Installation

1. `DepotDownloader` (from JavaSteam) handles downloads
2. Progress tracked in `DownloadingAppInfo` entity
3. Manifests parsed for file verification

### Cloud Saves

- `SteamAutoCloud` monitors for cloud sync opportunities
- `SteamCloud` handler manages upload/download

## Dependencies

- **JavaSteam** - Fork of JavaSteam library for Steam API
- **javasteam-depotdownloader** - Depot/download handling

## Tests

- `SteamAutoCloudTest.kt` - Cloud sync tests

## Notes

- `SteamService.kt` is very large (~3800 lines) and could benefit from decomposition
- Uses JavaSteam (a Java library) for low-level Steam protocol
- Most complex service due to Steam's rich feature set
