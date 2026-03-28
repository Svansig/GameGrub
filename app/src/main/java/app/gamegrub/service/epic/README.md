# Epic Games Service

Epic Games Store platform integration for GameNative.

## Architecture

```
EpicService (Coordinator)
    │
    ├── EpicManager            → Game library management
    ├── EpicAuthManager        → Authentication & credentials
    ├── EpicDownloadManager    → Download queue & progress
    ├── EpicCloudSavesManager  → Cloud save sync
    ├── EpicAuthClient         → OAuth HTTP client
    ├── EpicGameLauncher       → Game startup
    └── manifest/
        ├── EpicManifest       → Manifest data models
        ├── JsonManifestParser → JSON manifest parsing
        └── ManifestUtils      → Manifest utilities
```

## Files

| File                             | Purpose                         |
|----------------------------------|---------------------------------|
| `EpicService.kt`                 | Main Android foreground service |
| `EpicManager.kt`                 | Game library, metadata          |
| `EpicAuthManager.kt`             | OAuth flow, token management    |
| `EpicDownloadManager.kt`         | Download handling               |
| `EpicCloudSavesManager.kt`       | Cloud save synchronization      |
| `EpicAuthClient.kt`              | OAuth HTTP client               |
| `EpicGameLauncher.kt`            | Game startup logic              |
| `EpicConstants.kt`               | Platform constants              |
| `manifest/EpicManifest.kt`       | Manifest data models            |
| `manifest/JsonManifestParser.kt` | JSON parsing                    |
| `manifest/ManifestUtils.kt`      | Manifest utilities              |

## Database

- **Entity**: `EpicGame` - stored in Room database
- **DAO**: `EpicGameDao` - CRUD operations

## Key Flows

### Authentication

1. User initiates OAuth via Epic website
2. `EpicAuthManager` handles token exchange
3. Tokens stored securely

### Library Sync

1. `EpicService.start()` triggers sync
2. `EpicManager` fetches owned games
3. Results stored in Room

### Game Installation

1. User selects game to install
2. `EpicDownloadManager` queues download
3. Manifest parsed to determine required files

## Tests

- `EpicCloudSavesTest.kt` - Cloud saves tests
- `EpicManagerTest.kt` - Manager tests

## Notes

- Epic uses JSON manifests for game metadata
- Cloud saves sync handled by `EpicCloudSavesManager`
- Supports external display for games
