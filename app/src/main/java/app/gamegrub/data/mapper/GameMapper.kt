package app.gamegrub.data.mapper

import app.gamegrub.data.AmazonGame
import app.gamegrub.data.EpicGame
import app.gamegrub.data.GameSource
import app.gamegrub.data.GOGGame
import app.gamegrub.data.SteamApp
import app.gamegrub.data.UnifiedGame

object GameMapper {
    fun steamAppToUnified(app: SteamApp): UnifiedGame {
        return UnifiedGame(
            appId = "${GameSource.STEAM.name}_${app.id}",
            gameSource = GameSource.STEAM,
            name = app.name,
            iconUrl = app.iconUrl,
            headerUrl = app.headerUrl,
            isInstalled = app.installDir.isNotEmpty(),
            installPath = app.installDir,
            type = app.type,
            developer = app.developer,
            publisher = app.publisher,
            releaseDate = if (app.releaseDate > 0) app.releaseDate.toString() else "",
        )
    }

    fun gogGameToUnified(game: GOGGame): UnifiedGame {
        return UnifiedGame(
            appId = "${GameSource.GOG.name}_${game.id}",
            gameSource = GameSource.GOG,
            name = game.title,
            iconUrl = game.iconUrl,
            headerUrl = game.imageUrl,
            isInstalled = game.isInstalled,
            installPath = game.installPath,
            installSize = game.installSize,
            downloadSize = game.downloadSize,
            lastPlayed = game.lastPlayed,
            playTime = game.playTime,
            type = game.type,
            description = game.description,
            developer = game.developer,
            publisher = game.publisher,
            releaseDate = game.releaseDate,
        )
    }

    fun epicGameToUnified(game: EpicGame): UnifiedGame {
        return UnifiedGame(
            appId = "${GameSource.EPIC.name}_${game.id}",
            gameSource = GameSource.EPIC,
            name = game.title,
            iconUrl = game.iconUrl,
            headerUrl = game.artCover,
            isInstalled = game.isInstalled,
            installPath = game.installPath,
            installSize = game.installSize,
            downloadSize = game.downloadSize,
            lastPlayed = game.lastPlayed,
            playTime = game.playTime,
            type = game.type,
            description = game.description,
            developer = game.developer,
            publisher = game.publisher,
            releaseDate = game.releaseDate,
        )
    }

    fun amazonGameToUnified(game: AmazonGame): UnifiedGame {
        return UnifiedGame(
            appId = "${GameSource.AMAZON.name}_${game.productId}",
            gameSource = GameSource.AMAZON,
            name = game.title,
            iconUrl = game.artUrl,
            headerUrl = game.heroUrl,
            isInstalled = game.isInstalled,
            installPath = game.installPath,
            installSize = game.installSize,
            downloadSize = game.downloadSize,
            lastPlayed = game.lastPlayed,
            playTime = game.playTimeMinutes,
            developer = game.developer,
            publisher = game.publisher,
            releaseDate = game.releaseDate,
        )
    }

    fun unifiedToLibraryItem(game: UnifiedGame): app.gamegrub.data.LibraryItem {
        return app.gamegrub.data.LibraryItem(
            appId = game.appId,
            name = game.name,
            iconHash = game.iconUrl,
            headerImageUrl = game.headerUrl,
            gameSource = game.gameSource,
            sizeBytes = game.installSize,
            isInstalled = game.isInstalled,
        )
    }
}
