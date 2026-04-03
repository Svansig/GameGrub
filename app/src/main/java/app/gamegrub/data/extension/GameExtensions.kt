package app.gamegrub.data.extension

import app.gamegrub.data.AmazonGame
import app.gamegrub.data.EpicGame
import app.gamegrub.data.GameSource
import app.gamegrub.data.GOGGame
import app.gamegrub.data.LibraryItem
import app.gamegrub.data.SteamApp
import app.gamegrub.data.UnifiedGame

fun LibraryItem.toUnifiedGame(): UnifiedGame {
    return UnifiedGame(
        appId = appId,
        gameSource = gameSource,
        name = name,
        iconUrl = iconHash,
        headerUrl = headerImageUrl,
        isInstalled = isInstalled,
        installSize = sizeBytes,
    )
}

fun UnifiedGame.toLibraryItem(): LibraryItem {
    return LibraryItem(
        appId = appId,
        name = name,
        iconHash = iconUrl,
        headerImageUrl = headerUrl,
        heroImageUrl = headerUrl,
        gameSource = gameSource,
        sizeBytes = installSize,
        isInstalled = isInstalled,
    )
}

fun SteamApp.toUnifiedGame(): UnifiedGame {
    return UnifiedGame(
        appId = "${GameSource.STEAM.name}_$id",
        gameSource = GameSource.STEAM,
        name = name,
        iconUrl = iconUrl,
        headerUrl = headerUrl,
        isInstalled = installDir.isNotEmpty(),
        installPath = installDir,
        type = type,
        developer = developer,
        publisher = publisher,
    )
}

fun GOGGame.toUnifiedGame(): UnifiedGame {
    return UnifiedGame(
        appId = "${GameSource.GOG.name}_$id",
        gameSource = GameSource.GOG,
        name = title,
        iconUrl = iconUrl,
        headerUrl = imageUrl,
        isInstalled = isInstalled,
        installPath = installPath,
        installSize = installSize,
        downloadSize = downloadSize,
        lastPlayed = lastPlayed,
        playTime = playTime,
        type = type,
        description = description,
        developer = developer,
        publisher = publisher,
        releaseDate = releaseDate,
    )
}

fun EpicGame.toUnifiedGame(): UnifiedGame {
    return UnifiedGame(
        appId = "${GameSource.EPIC.name}_$id",
        gameSource = GameSource.EPIC,
        name = title,
        iconUrl = iconUrl,
        headerUrl = artCover,
        isInstalled = isInstalled,
        installPath = installPath,
        installSize = installSize,
        downloadSize = downloadSize,
        lastPlayed = lastPlayed,
        playTime = playTime,
        type = type,
        description = description,
        developer = developer,
        publisher = publisher,
        releaseDate = releaseDate,
    )
}

fun AmazonGame.toUnifiedGame(): UnifiedGame {
    return UnifiedGame(
        appId = "${GameSource.AMAZON.name}_$productId",
        gameSource = GameSource.AMAZON,
        name = title,
        iconUrl = artUrl,
        headerUrl = heroUrl,
        isInstalled = isInstalled,
        installPath = installPath,
        installSize = installSize,
        downloadSize = downloadSize,
        lastPlayed = lastPlayed,
        playTime = playTimeMinutes,
        developer = developer,
        publisher = publisher,
        releaseDate = releaseDate,
    )
}

val UnifiedGame.storeSpecificId: String
    get() = appId.removePrefix("${gameSource.name}_")
