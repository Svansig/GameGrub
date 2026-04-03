package app.gamegrub.test

import app.gamegrub.data.GameSource
import app.gamegrub.data.LibraryItem

object GameTestData {
    fun libraryItem(
        appId: String = "STEAM_12345",
        name: String = "Test Game",
        gameSource: GameSource = GameSource.STEAM,
        isInstalled: Boolean = false,
    ): LibraryItem {
        return LibraryItem(
            appId = appId,
            name = name,
            iconHash = "https://example.com/icon.png",
            headerImageUrl = "https://example.com/header.png",
            gameSource = gameSource,
            sizeBytes = 1024 * 1024 * 1024L,
            isInstalled = isInstalled,
        )
    }

    fun libraryItemList(
        count: Int = 10,
        gameSource: GameSource = GameSource.STEAM,
    ): List<LibraryItem> {
        return (1..count).map { i ->
            libraryItem(
                appId = "${gameSource.name}_$i",
                name = "Test Game $i",
                gameSource = gameSource,
                isInstalled = i % 2 == 0,
            )
        }
    }

    fun installedGamesList(gameSource: GameSource = GameSource.STEAM): List<LibraryItem> {
        return listOf(
            libraryItem("${gameSource.name}_1", "Installed Game 1", gameSource, true),
            libraryItem("${gameSource.name}_2", "Installed Game 2", gameSource, true),
            libraryItem("${gameSource.name}_3", "Installed Game 3", gameSource, true),
        )
    }

    fun uninstalledGamesList(gameSource: GameSource = GameSource.STEAM): List<LibraryItem> {
        return listOf(
            libraryItem("${gameSource.name}_4", "Uninstalled Game 1", gameSource, false),
            libraryItem("${gameSource.name}_5", "Uninstalled Game 2", gameSource, false),
        )
    }

    fun mixedGamesList(): List<LibraryItem> {
        return installedGamesList(GameSource.STEAM) + 
               installedGamesList(GameSource.GOG) +
               uninstalledGamesList(GameSource.EPIC)
    }
}

object AuthTestData {
    fun isLoggedIn(source: GameSource): Boolean {
        return source in setOf(GameSource.STEAM, GameSource.GOG)
    }

    fun loggedInStores(): Set<GameSource> {
        return setOf(GameSource.STEAM, GameSource.GOG)
    }
}
