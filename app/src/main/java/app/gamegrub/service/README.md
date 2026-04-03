# Service Layer

Android foreground services and business logic managers.

## Architecture

```
service/
├── base/
│   └── GameStoreService.kt    # Base class for all services
├── auth/
│   └── GameStoreAuth.kt       # Auth interface
├── download/
│   └── GameStoreDownloader.kt # Download interface
├── cloud/
│   └── GameStoreCloudSaves.kt # Cloud saves interface
├── factory/
│   └── GameStoreServiceFactory.kt # Service factory
├── steam/                     # Steam service & domains
├── gog/                       # GOG service
├── epic/                      # Epic service
└── amazon/                    # Amazon service
```

## Base Class: GameStoreService

All services extend `GameStoreService` which provides:

- Service lifecycle management
- Sync throttling (15 min default)
- Foreground notification
- Abstract methods to implement:
    - `getServiceTag()` - Logging tag
    - `performSync(context, isManual)` - Library sync
    - `getNotificationTitle()` - Notification title
    - `getNotificationContent()` - Notification content

```kotlin
class MyService : GameStoreService() {
    override fun getServiceTag() = "MY_SERVICE"

    override fun performSync(context: Context, isManual: Boolean) {
        // Sync logic here
    }

    override fun getNotificationTitle() = "My Service"

    override fun getNotificationContent() = "Running"
}
```

## Service Interfaces

Unified interfaces for cross-store operations:

- `GameStoreAuth` - Authentication
- `GameStoreDownloader` - Downloads
- `GameStoreCloudSaves` - Cloud saves

## Managers

Each service delegates to specialized managers:

- `*Manager` - Core business logic
- `*AuthManager` - Authentication
- `*DownloadManager` - Downloads
- `*CloudSavesManager` - Cloud saves

## Steam Domains

Steam service uses domain pattern for business logic:

- `SteamLibraryDomain` - Library management
- `SteamAccountDomain` - Account operations
- `SteamInstallDomain` - Installation
- `SteamSessionDomain` - Session/launch
- `SteamCloudStatsDomain` - Cloud stats
- `SteamPicsSyncDomain` - PICS synchronization
