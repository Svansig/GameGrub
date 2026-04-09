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

**Dependency Flow:**
```
UI Layer → Service Layer → Data Layer
     ↓            ↓            ↓
  Utils Layer ← Utilizes ←  Utils Layer
```

**Entry Points:**
- `GameGrubApp` - Application class, initializes Hilt, Timber, NetworkManager, CrashHandler, PrefManager, PostHog, PlayIntegrity
- `MainActivity` - Main activity, handles deep links (`home://gamegrub`) and external game launch intents

Manifest and policy-sensitive permission rationale are tracked in `docs/android-manifest-audit.md`.

### 2. UI Layer (`app.gamegrub.ui`)

```
ui/
├── screen/          # Compose screens (Library, Settings, etc.)
├── component/       # Reusable Compose components
├── theme/           # Material3 theming
├── widget/          # Custom Android views (PerformanceHud)
├── model/           # ViewModels (@HiltViewModel)
└── utils/           # UI utilities
```

UI placement and boundary cleanup guidance is tracked in `docs/ui-placement/`.

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
| **Network** | `network/NetworkManager`, `UpdateChecker` |
| **Auth** | `PlatformAuthUtils`, `PlayIntegrity` |
| **General** | `DateTimeUtils`, `StringUtils`, `MathUtils` |

**Dependency Guardrails:**
- Utils packages should depend on service/domain layers, not vice versa
- UI-layer utils (`ui/utils/`) should only depend on UI models, not service or domain
- Business-logic utilities in `app.gamegrub.utils` can depend on service/domain but should not depend on UI
- Platform-specific utils (Steam, GOG, etc.) should encapsulate platform adapters

**Network boundary:**
- `app.gamegrub.network.NetworkManager` is the central owner for network infrastructure concerns (shared OkHttp client setup, DNS fallback, connectivity checks, coroutine-friendly request execution).
- Platform/service code should keep business logic and API semantics, while delegating transport/infra concerns to `NetworkManager`.
- Legacy `NetworkMonitor` and `utils/network/Net` compatibility wrappers were removed after migration; new network infrastructure must use `NetworkManager` directly.

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

| Event | Purpose | Owner | Consumer |
|-------|---------|-------|----------|
| `AndroidEvent` | Activity lifecycle, system UI, key events | App Layer | ViewModels, UI screens |
| `SteamEvent` | Steam connection, login, game events | SteamService | ViewModels, UI screens |

**Subscription Lifecycle:**
- UI subscriptions should use `DisposableEffect` for cleanup
- ViewModel subscriptions use `viewModelScope` for automatic cleanup
- Services emit events; UI subscribes to observe state changes

**Usage:**
```kotlin
// Subscribe (with cleanup)
LaunchedEffect(Unit) {
    XServerRuntime.get().events.on<AndroidEvent.BackPressed, Unit> {
        // Handle back press
    }
}
DisposableEffect(Unit) {
    onDispose {
        XServerRuntime.get().events.off<AndroidEvent.BackPressed, Unit>(handler)
    }
}

// Emit
XServerRuntime.get().events.emit(AndroidEvent.BackPressed)
```

## Orientation Ownership Contract

Orientation behavior is centrally owned by `OrientationManager` and lifecycle-owned by `MainActivity`.

- `MainActivity` starts/stops orientation sensing in `onStart`/`onStop`.
- `MainActivity` is the only component that writes `Activity.requestedOrientation` (through `OrientationManager`).
- UI and ViewModel layers emit `AndroidEvent.SetOrientationPolicy` with an `OrientationPolicy` payload.
- `OrientationPolicy` merges three orientation inputs:
  - `userAllowedOrientations` (saved preference)
  - `sessionOverrideOrientations` (route/session override, e.g., portrait-only container)
  - `fallbackOrientations` (final fallback, usually `UNSPECIFIED`)

Policy precedence is: `sessionOverrideOrientations` -> `userAllowedOrientations` -> `fallbackOrientations`.

This contract prevents direct orientation writes from composables and keeps route-specific behavior explicit and testable.

## System UI Ownership Contract

System bar visibility and immersive-mode writes are centrally owned by `ImmersiveModeManager`.

- `MainActivity` owns a single `ImmersiveModeManager` instance.
- UI and screen layers request visibility intent through `AndroidEvent.SetSystemUIVisibility`.
- `ImmersiveModeManager` is the only owner that applies:
  - `WindowCompat.setDecorFitsSystemWindows(...)`
  - status/navigation bar `show(...)`/`hide(...)`
  - system-bars behavior policy for swipe-to-reveal transient bars
- Legacy helpers (for example `AppUtils.hideSystemUI(...)`) must delegate to `ImmersiveModeManager` and must not write insets directly.

This contract keeps platform-specific insets behavior in one place and reduces lifecycle/focus regressions.

## Device Query Ownership Contract

Device and hardware queries are centralized behind `DeviceQueryGateway` with `AndroidDeviceQueryManager` as the Android implementation.

- Only the device query manager may directly read:
  - Android identity APIs (`Build`, `Settings.*`)
  - runtime hardware APIs (`ActivityManager`, `BatteryManager`)
  - low-level probe sources (`/proc`, `/sys` thermal/KGSL)
  - Winlator GPU probes (`GPUInformation`)
- App/service/UI callers consume the gateway contract and avoid direct hardware API access.
- Legacy wrappers (`DeviceUtils`, `HardwareUtils`) are removed as part of this consolidation.

This keeps Android 13+ device-behavior assumptions in one maintainable boundary and improves testability.

## Storage Ownership Contract

Storage-related tasks are centrally owned by `app.gamegrub.storage.StorageManager` and storage package abstractions.

- Callers outside `app.gamegrub.storage` must not perform direct storage policy logic.
- Marker lifecycle, filesystem reads/writes, file traversal, and storage-capacity operations are delegated to storage package APIs.
- Legacy utility wrappers in `utils/storage` are removed; callers now use storage package ownership paths.

Migration rule: new storage changes must be implemented in the storage package first, then consumed by service/UI callers via that boundary.

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
- **Min SDK**: 33
- **Target SDK**: 35
- **Java**: 17
- **NDK**: 27.1.11397112

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
- [Architecture Assessment (2026-04-09)](./docs/architecture-assessment-2026-04-09.md) - Current architecture scorecard and roadmap
