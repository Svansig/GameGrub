# Amazon Games Service

Amazon Games (Amazon Prime Gaming) platform integration for GameNative.

## Architecture

```
AmazonService (Coordinator)
    │
    ├── AmazonManager           → Game library management
    ├── AmazonAuthManager       → Authentication & credentials
    ├── AmazonDownloadManager   → Download queue & progress
    ├── AmazonApiClient         → HTTP API client
    ├── AmazonAuthClient        → OAuth HTTP client
    ├── AmazonManifest          → Manifest handling
    ├── AmazonSdkManager        → Amazon SDK integration
    └── AmazonPKCEGenerator     → PKCE code generation
```

## Files

| File                       | Purpose                         |
|----------------------------|---------------------------------|
| `AmazonService.kt`         | Main Android foreground service |
| `AmazonManager.kt`         | Game library management         |
| `AmazonAuthManager.kt`     | Authentication & credentials    |
| `AmazonDownloadManager.kt` | Download handling               |
| `AmazonApiClient.kt`       | HTTP API client                 |
| `AmazonAuthClient.kt`      | OAuth HTTP client               |
| `AmazonManifest.kt`        | Manifest parsing                |
| `AmazonSdkManager.kt`      | Amazon SDK integration          |
| `AmazonConstants.kt`       | Platform constants              |
| `AmazonPKCEGenerator.kt`   | PKCE code generation            |

## Database

- **Entity**: `AmazonGame` - stored in Room database
- **DAO**: `AmazonGameDao` - CRUD operations

## Key Flows

### Authentication

1. User initiates OAuth/PKCE flow
2. `AmazonAuthManager` handles token exchange
3. Credentials stored securely

### Library Sync

1. `AmazonService.start()` triggers sync
2. `AmazonManager` fetches owned games
3. Results stored in Room

## Tests

- `AmazonManifestTest.kt` - Manifest parsing tests

## Notes

- Amazon Games uses a different manifest format
- Requires PKCE for OAuth (see `AmazonPKCEGenerator`)
- Integrates with Amazon's SDK for some features
