# ARCH-001a - Define Unified Game Interface and Entity Schema

- **ID**: `ARCH-001a`
- **Area**: `data/domain`
- **Priority**: `P0`
- **Status**: `Done`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Each game store has its own data entity (SteamApp, GOGGame, EpicGame, AmazonGame) with nearly identical fields causing duplication and lack of single source of truth.

## Scope

- In scope:
  - Analyze existing entities (SteamApp, GOGGame, EpicGame, AmazonGame) 
  - Define unified `Game` interface with common fields
  - Design Room entity schema with GameSource discriminator
  - Handle store-specific fields via nullable columns or JSON blob
- Out of scope:
  - Database migration (covered in ARCH-001b)
  - Consumer updates

## Dependencies and Decomposition

- Parent ticket: `ARCH-001`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Analyze all 4 existing entities and document common vs unique fields
- [x] Analysis complete: Common fields identified as id, name/title, isInstalled, installPath, installSize, downloadSize, lastPlayed, playTime, type, iconUrl, description, developer, publisher, releaseDate
- [x] Store-specific fields identified: SteamApp has rich depot/license/config; GOG has slug/genres/languages; Epic has namespace/catalogId/platform/version/EOS; Amazon has productId/entitlementId/productJson
- [ ] Create `Game` interface with: id, appId, name, iconUrl, headerImageUrl, gameSource, isInstalled, installPath, installSize, lastPlayed, playTime, compatibilityStatus
- [ ] Design Room schema with GameSource enum as discriminator
- [ ] Handle store-specific fields (e.g., SteamDepotInfo, GOGManifestInfo, EpicManifestInfo, AmazonManifestInfo)
- [ ] Documentation of design decisions

## Analysis Complete

### Common Fields (to be in unified Game entity):
| Field | SteamApp | GOGGame | EpicGame | AmazonGame |
|-------|----------|---------|----------|------------|
| id | id (Int) | id (String) | id (Int auto) | appId (Int auto) |
| name | name | title | title | title |
| iconUrl | iconHash, clientIconHash | iconUrl | artSquare, artCover | artUrl |
| headerImage | headerImage | imageUrl | artCover, artPortrait | heroUrl |
| isInstalled | (derived from installDir) | isInstalled | isInstalled | isInstalled |
| installPath | installDir | installPath | installPath | installPath |
| installSize | (derived) | installSize | installSize | installSize |
| downloadSize | - | downloadSize | downloadSize | downloadSize |
| lastPlayed | - | lastPlayed | lastPlayed | lastPlayed |
| playTime | - | playTime | playTime | playTimeMinutes |
| type | type (AppType) | type | type | - |
| description | - | description | description | - |
| developer | developer | developer | developer | developer |
| publisher | publisher | publisher | publisher | publisher |
| releaseDate | releaseDate | releaseDate | releaseDate | releaseDate |

### Store-Specific Fields (to be in JSON blob):
- **SteamApp**: depots, branches, packageId, licenseFlags, config, ufs, etc.
- **GOGGame**: slug, genres, languages, exclude
- **EpicGame**: catalogId, appName, namespace, platform, version, executable, cloudSaveEnabled, isDLC, eos fields
- **AmazonGame**: productId, entitlementId, versionId, productSku, productJson

## Implementation Design

### Proposed Unified Game Entity
```kotlin
@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // App ID - unique within GameNative (format: "STEAM_12345", "GOG_abc123", etc.)
    @ColumnInfo(name = "app_id")
    val appId: String,
    
    // Store identifier
    @ColumnInfo(name = "game_source")
    val gameSource: GameSource,
    
    // Common fields
    @ColumnInfo(name = "name")
    val name: String = "",
    
    @ColumnInfo(name = "icon_url")
    val iconUrl: String = "",
    
    @ColumnInfo(name = "header_url")
    val headerUrl: String = "",
    
    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean = false,
    
    @ColumnInfo(name = "install_path")
    val installPath: String = "",
    
    @ColumnInfo(name = "install_size")
    val installSize: Long = 0,
    
    @ColumnInfo(name = "download_size")
    val downloadSize: Long = 0,
    
    @ColumnInfo(name = "last_played")
    val lastPlayed: Long = 0,
    
    @ColumnInfo(name = "play_time")
    val playTime: Long = 0,
    
    @ColumnInfo(name = "type")
    val type: AppType = AppType.game,
    
    @ColumnInfo(name = "description")
    val description: String = "",
    
    @ColumnInfo(name = "developer")
    val developer: String = "",
    
    @ColumnInfo(name = "publisher")
    val publisher: String = "",
    
    @ColumnInfo(name = "release_date")
    val releaseDate: String = "",
    
    // Store-specific data as JSON
    @ColumnInfo(name = "store_data")
    val storeData: String = "", // JSON string for store-specific fields
)
```

### Migration Strategy
1. **Version 14**: Add new `games` table with unified schema
2. **Data migration**: Copy data from legacy tables (steam_app, gog_games, epic_games, amazon_games)
3. **Version 15**: Drop legacy tables (or keep for backward compatibility during transition)

## Validation

- [ ] Relevant unit/integration tests pass.
- [ ] `./gradlew lintKotlin` passes for touched files.
- [ ] Design review with team (documented in ticket)
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
