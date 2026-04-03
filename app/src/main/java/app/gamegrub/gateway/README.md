# Gateway Layer

Provides abstraction boundaries between UI and service layers.

## Purpose

Gateways decouple UI from direct service access, enabling:

- Dependency injection for testability
- Unified API across different game stores
- Easy mocking in tests

## Gateway Types

| Gateway              | Purpose                             |
|----------------------|-------------------------------------|
| `LibraryGateway`     | Query and filter game library       |
| `AuthGateway`        | Authentication state and operations |
| `LaunchGateway`      | Game launch orchestration           |
| `DownloadGateway`    | Download management                 |
| `CloudSavesGateway`  | Cloud save sync                     |
| `PreferencesGateway` | App preferences                     |
| `StorageGateway`     | File system operations              |

## Usage

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val libraryGateway: LibraryGateway,
    private val launchGateway: LaunchGateway,
) {
    fun getGames() = libraryGateway.getAllGames()

    suspend fun launchGame(game: LibraryItem) = launchGateway.launchGame(game)
}
```

## Implementation

Implementations live in `gateway/impl/` and are bound in `di/GatewayModule`.
