# GOG Service

GOG (Good Old Games) platform integration for GameNative.

## Architecture

```
GOGService (Coordinator)
    │
    ├── GOGManager          → Game library management, installation
    ├── GOGAuthManager      → Authentication & credentials
    ├── GOGDownloadManager → Download queue & progress
    ├── GOGCloudSavesManager→ Cloud save sync
    ├── GOGApiClient        → HTTP API client
    └── GOGManifestParser   → Game manifest parsing
```

## Files

| File | Purpose |
|------|---------|
| `GOGService.kt` | Main Android foreground service |
| `GOGManager.kt` | Game library, installation, launching |
| `GOGAuthManager.kt` | OAuth flow, token management |
| `GOGDownloadManager.kt` | Download handling with progress |
| `GOGCloudSavesManager.kt` | Cloud save synchronization |
| `GOGApiClient.kt` | HTTP client for GOG APIs |
| `GOGConstants.kt` | Platform constants |
| `GOGManifestUtils.kt` | Manifest utilities |
| `api/GOGApiClient.kt` | Legacy - duplicate API client |
| `api/GOGDataModels.kt` | API response models |
| `api/GOGManifestParser.kt` | Manifest parsing |

## Database

- **Entity**: `GOGGame` - stored in Room database
- **DAO**: `GOGGameDao` - CRUD operations

## Key Flows

### Authentication
1. User initiates OAuth via GOG website
2. `GOGAuthManager` handles token exchange
3. Credentials stored securely

### Library Sync
1. `GOGService.start()` triggers sync
2. `GOGApiClient` fetches owned games
3. Results stored in Room via `GOGGameDao`
4. UI observes changes via Flow

### Game Installation
1. User selects game to install
2. `GOGDownloadManager` queues download
3. Progress updates via notification
4. On complete: game executable registered

## Tests

- `GOGAuthManagerTest.kt` - Authentication tests
- `GOGManifestParserTest.kt` - Manifest parsing tests

## Notes

- GOG has its own manifest format (different from Steam/Epic)
- Cloud saves handled via GOG's Galaxy API
- Some code duplication exists between `gog/` and `gog/api/` packages
