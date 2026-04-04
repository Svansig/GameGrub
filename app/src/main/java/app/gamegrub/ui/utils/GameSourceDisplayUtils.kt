package app.gamegrub.ui.utils

import app.gamegrub.R
import app.gamegrub.data.GameSource

/**
 * UI presentation utilities for game source display names and icons.
 *
 * These helpers are purely UI-facing and should be used only from Compose/UI layers.
 * For data-layer source identification, use [GameSource] enum directly.
 *
 * @see GameSource
 */
object GameSourceDisplayUtils {

    /**
     * Returns the user-friendly display name for a game source.
     *
     * @param source The game source to display
     * @return Human-readable display name (e.g., "Steam", "Epic Games Store")
     */
    fun getDisplayName(source: GameSource): String {
        return when (source) {
            GameSource.STEAM -> "Steam"
            GameSource.GOG -> "GOG"
            GameSource.EPIC -> "Epic Games Store"
            GameSource.AMAZON -> "Amazon Games"
            GameSource.CUSTOM_GAME -> "Custom Game"
        }
    }

    /**
     * Returns the short technical name for a game source.
     *
     * @param source The game source
     * @return Short name (matches enum name)
     */
    fun getShortName(source: GameSource): String {
        return source.name
    }

    /**
     * Returns the drawable resource ID for the game source icon.
     *
     * Currently all sources use the same generic logo - this may be updated
     * with source-specific icons in the future.
     *
     * @param source The game source
     * @return Drawable resource ID for the source icon
     */
    fun getIconResource(source: GameSource): Int {
        return when (source) {
            GameSource.STEAM -> R.drawable.ic_logo_color
            GameSource.GOG -> R.drawable.ic_logo_color
            GameSource.EPIC -> R.drawable.ic_logo_color
            GameSource.AMAZON -> R.drawable.ic_logo_color
            GameSource.CUSTOM_GAME -> R.drawable.ic_logo_color
        }
    }
}
