# Data Layer

Contains domain models, entities, and data access.

## Structure

```
data/
├── UnifiedGame.kt       # Unified game model with GameSource
├── GameSource.kt        # Enum: STEAM, GOG, EPIC, AMAZON, CUSTOM
├── LibraryItem.kt       # UI model for game display
├── DownloadInfo.kt      # Download progress tracking
├── LaunchInfo.kt        # Game launch parameters
├── GameFilter.kt        # Filtering options
├── GameDetails.kt       # Extended game info
├── extension/           # Extension functions
├── mapper/              # Entity mappers
├── repository/          # Repository pattern
└── sync/                # Data synchronization
```

## Models

### UnifiedGame
Primary model for all games, uses `GameSource` discriminator:
```kotlin
data class UnifiedGame(
    val id: Int = 0,
    val appId: String,
    val name: String,
    val gameSource: GameSource,
    val isInstalled: Boolean = false,
    val installPath: String = "",
    // ... additional fields
)
```

### GameSource
Discriminator for game stores:
- `STEAM` - Steam platform
- `GOG` - GOG platform
- `EPIC` - Epic Games Store
- `AMAZON` - Amazon Prime Gaming
- `CUSTOM_GAME` - User-added games

## Repository

`GameRepository` provides single source of truth for game data:
```kotlin
@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao
) {
    fun getAllGames(): Flow<List<UnifiedGame>>
    fun getGamesBySource(source: GameSource): Flow<List<UnifiedGame>>
    fun getInstalledGames(): Flow<List<UnifiedGame>>
    suspend fun getGameByAppId(appId: String): UnifiedGame?
    // ...
}
```
