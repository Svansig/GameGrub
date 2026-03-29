# GameNative Architecture

This document explains the high-level architecture of GameNative for developers.

## What is GameNative?

GameNative is an Android application that lets you play Windows games from Steam, GOG, Epic, and Amazon on Android devices. It works by running Windows games in a Wine/Proton container with an X11 server, essentially turning your Android device into a remote gaming console.

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        MainActivity                              │
│  (Entry point, handles deep links, game launch intents)         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Compose UI Layer                             │
│  Screens → ViewModels → StateFlow → Compose UI                   │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
   │SteamService │     │ GOGService  │     │EpicService │
   │AmazonService│     │             │     │             │
   └─────────────┘     └─────────────┘     └─────────────┘
          │                   │                   │
          └───────────────────┼───────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Wine/Proton Container                         │
│              (XServer + Wine + dxvk/vulkan)                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Android OS                                   │
│        (Networking, Storage, Notifications, etc.)               │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Application Layer (`app.gamegrub`)

**Entry Points:**
- `GameGrubApp` - Application class, initializes Hilt, Timber, NetworkMonitor, CrashHandler, PrefManager, PostHog, PlayIntegrity
- `MainActivity` - Main activity, handles deep links (`home://gamegrub`) and external game launch intents

### 2. UI Layer (`app.gamegrub.ui`)

```
ui/
├── screen/          # Compose screens (Library, Settings, etc.)
├── components/      # Reusable Compose components
├── theme/           # Material3 theming
├── widget/          # Custom Android views (PerformanceHud)
├── model/           # ViewModels (@HiltViewModel)
└── util/            # UI utilities
```

**Pattern:** MVVM with Jetpack Compose
- ViewModels expose `StateFlow` for UI state
- Compose screens collect state via `collectAsStateWithLifecycle()`
- User actions flow back through ViewModel methods

### 3. Service Layer (`app.gamegrub.service`)

Each gaming platform has its own service that runs as a **foreground service**:

| Service | Package | Purpose |
|---------|---------|---------|
| **SteamService** | `app.gamegrub.service` | Steam authentication, game library, downloads, cloud saves |
| **GOGService** | `app.gamegrub.service.gog` | GOG authentication, library, downloads |
| **EpicService** | `app.gamegrub.service.epic` | Epic authentication, library, downloads |
| **AmazonService** | `app.gamegrub.service.amazon` | Amazon authentication, library, downloads |

**Common Service Architecture:**

Each service follows a coordinator pattern:

```
┌──────────────────┐
│   XxxService    │  ← Main service (Android Service)
│  (Coordinator)  │
└────────┬─────────┘
         │ delegates to
         ▼
┌──────────────────┐
│   XxxManager    │  ← Business logic
└────────┬─────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌───────┐
│ Api   │ │Download│
│Client │ │Manager │
└───────┘ └───────┘
```

**Supporting Services:**
- `DownloadService` - Shared download queue across all platforms
- `NotificationHelper` - Centralized notification management
- `AchievementWatcher` - Monitors Steam achievements

### 4. Data Layer (`app.gamegrub.data`, `app.gamegrub.db`)

```
data/           # Entity classes (SteamApp, GOGGame, etc.)
db/
├── dao/        # Room DAOs (data access)
├── converters/ # Type converters for complex types
└── serializers # Kotlinx Serialization serializers
```

**Room Database Schema (v13):**

| Entity | DAO | Description |
|--------|-----|-------------|
| `SteamApp` | `SteamAppDao` | Installed Steam games |
| `SteamLicense` | `SteamLicenseDao` | Game licenses/ownership |
| `GOGGame` | `GOGGameDao` | GOG games |
| `EpicGame` | `EpicGameDao` | Epic games |
| `AmazonGame` | `AmazonGameDao` | Amazon games |
| `AppInfo` | `AppInfoDao` | Steam app metadata |
| `CachedLicense` | `CachedLicenseDao` | Cached license data |
| `DownloadingAppInfo` | `DownloadingAppInfoDao` | Active downloads |

### 5. Utility Layer (`app.gamegrub.utils`)

**Note:** This folder contains 50+ files and is a candidate for reorganization. Current categories:

| Category | Examples |
|----------|----------|
| **Container** | `ContainerUtils`, `ContainerMigrator`, `LaunchDependencies` |
| **Steam** | `SteamUtils`, `SteamTokenHelper`, `SteamControllerVdfUtils` |
| **Game Data** | `GameMetadataManager`, `CustomGameScanner`, `GameCompatibilityService` |
| **Storage** | `FileUtils`, `StorageUtils`, `KeyValueUtils` |
| **Network** | `NetworkUtils`, `UpdateChecker` |
| **Auth** | `PlatformAuthUtils`, `PlayIntegrity` |
| **General** | `DateTimeUtils`, `StringUtils`, `MathUtils` |

### 6. Legacy Layer (`com.winlator`)

Inherited from the Pluvia fork - the core XServer/Wine container implementation:

```
com.winlator/
├── xserver/           # X11 server implementation
├── renderer/         # Graphics rendering (dxvk, vulkan)
├── container/        # Wine/Proton container management
├── box86_64/         # Wine compatibility layer (x86 on ARM)
├── inputcontrols/    # Controller input handling
├── widget/           # Custom Android views
│   ├── XServerView   # Main game display view
│   ├── TouchpadView # Virtual touchpad
│   └── InputControlsView
└── xenvironment/     # X environment configuration
```

**Warning:** This code has no tests and limited documentation. Proceed with caution when modifying.

## Dependency Injection (Hilt)

The app uses Hilt throughout:

| Annotation | Usage |
|------------|-------|
| `@HiltAndroidApp` | Application class |
| `@AndroidEntryPoint` | Activities, Services |
| `@HiltViewModel` | ViewModels |
| `@Inject` | Field/constructor injection |
| `@Module` | DI configuration modules |

**Modules:**
- `DatabaseModule` - Room database and DAOs
- `AppThemeModule` - Theme configuration

## Event System (`app.gamegrub.events`)

Custom event dispatcher for app-wide events:

| Event | Purpose |
|-------|---------|
| `AndroidEvent` | Activity lifecycle, system UI, key events |
| `SteamEvent` | Steam-specific events |

**Usage:**
```kotlin
// Subscribe
GameGrubApp.events.on<AndroidEvent.BackPressed, Unit> {
    // Handle back press
}

// Emit
GameGrubApp.events.emit(AndroidEvent.BackPressed)
```

## Data Flow Examples

### 1. Launching a Game

```
User taps "Play" 
    → ViewModel calls service.launchGame()
    → Service creates Wine container
    → XServer starts
    → Game process spawns
    → AchievementWatcher monitors
```

### 2. Syncing Game Library

```
User logs in / App starts
    → Service.start() called
    → Service fetches credentials from storage
    → API client calls platform API
    → Results parsed and stored in Room
    → UI observes DAO changes via Flow
    → Screen updates automatically
```

### 3. Downloading a Game

```
User taps "Install"
    → DownloadService queues download
    → Platform-specific download manager handles it
    → Progress updates via notification
    → On complete: game stored, DB updated
```

## Key Technologies

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.1.21 |
| UI | Jetpack Compose (BOM 2025.01.01) |
| DI | Dagger Hilt 2.55 |
| Database | Room 2.8.4 |
| Networking | OkHttp 5.1.0 + JavaSteam |
| Images | Coil + Landscapist |
| Logging | Timber |
| Analytics | PostHog |
| Integrity | Google Play Integrity |

## Build Configuration

- **Compile SDK**: 35
- **Min SDK**: 26
- **Target SDK**: 28
- **Java**: 17
- **NDK**: 22.1.7171670

## Testing

- **Unit Tests**: JUnit 4 + MockK + Robolectric
- **Instrumented Tests**: AndroidJUnit4 + Espresso
- **Coverage Gaps**: No ViewModel tests, limited service tests, legacy code untested

## Contributing

When adding new features:

1. **New platform service**: Follow the coordinator pattern (Service → Manager → ApiClient/DownloadManager)
2. **Database changes**: Create Room migration, update schema version
3. **UI changes**: Use MVVM with StateFlow
4. **Tests**: Add unit tests for new utilities/services
5. **Documentation**: Update this file and add KDoc comments

## Further Reading

- [AGENTS.md](./AGENTS.md) - Developer commands and conventions
- [README.md](./README.md) - Project overview
