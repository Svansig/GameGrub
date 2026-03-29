package app.gamegrub.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.materialkolor.PaletteStyle

/**
 * Custom color system for GameGrub, extending Material3.
 * Provides app-specific colors beyond the Material ColorScheme.
 */
@Immutable
data class GameGrubColors(
    // Status colors
    val statusInstalled: Color,
    val statusDownloading: Color,
    val statusAvailable: Color,
    val statusAway: Color,
    val statusOffline: Color,

    // Friend status
    val friendOnline: Color,
    val friendOffline: Color,
    val friendInGame: Color,
    val friendAwayOrSnooze: Color,
    val friendInGameAwayOrSnooze: Color,
    val friendBlocked: Color,

    // Accents
    val accentCyan: Color,
    val accentPurple: Color,
    val accentPink: Color,
    val accentSuccess: Color,
    val accentWarning: Color,
    val accentDanger: Color,

    // Surfaces
    val surfacePanel: Color,
    val surfaceElevated: Color,

    // Utility
    val borderDefault: Color,
    val textMuted: Color,

    // Compatibility
    val compatibilityGood: Color,
    val compatibilityGoodBackground: Color,
    val compatibilityPartial: Color,
    val compatibilityPartialBackground: Color,
    val compatibilityUnknown: Color,
    val compatibilityUnknownBackground: Color,
    val compatibilityBad: Color,
    val compatibilityBadBackground: Color,
)

/**
 * Dark theme color palette.
 */
private val DarkGameGrubColors = GameGrubColors(
    statusInstalled = StatusInstalled,
    statusDownloading = StatusDownloading,
    statusAvailable = StatusAvailable,
    statusAway = StatusAway,
    statusOffline = StatusOffline,

    friendOnline = FriendOnline,
    friendOffline = FriendOffline,
    friendInGame = FriendInGame,
    friendAwayOrSnooze = FriendAwayOrSnooze,
    friendInGameAwayOrSnooze = FriendInGameAwayOrSnooze,
    friendBlocked = FriendBlocked,

    accentCyan = GameGrubCyan,
    accentPurple = GameGrubPurple,
    accentPink = GameGrubPink,
    accentSuccess = GameGrubSuccess,
    accentWarning = GameGrubWarning,
    accentDanger = GameGrubDanger,

    surfacePanel = GameGrubSurface,
    surfaceElevated = GameGrubSurfaceElevated,

    borderDefault = GameGrubBorder,
    textMuted = GameGrubForegroundMuted,

    compatibilityGood = CompatibilityGood,
    compatibilityGoodBackground = CompatibilityGoodBg,
    compatibilityPartial = CompatibilityPartial,
    compatibilityPartialBackground = CompatibilityPartialBg,
    compatibilityUnknown = CompatibilityUnknown,
    compatibilityUnknownBackground = CompatibilityUnknownBg,
    compatibilityBad = CompatibilityBad,
    compatibilityBadBackground = CompatibilityBadBg,
)

// Light theme placeholder - customize when adding light theme support
// private val LightGameGrubColors = GameGrubColors(...)

private val LocalGameGrubColors = staticCompositionLocalOf { DarkGameGrubColors }

/**
 * Material3 dark color scheme using GameGrub colors.
 */
private val DarkColorScheme = darkColorScheme(
    primary = GameGrubPrimary,
    onPrimary = GameGrubForeground,
    primaryContainer = GameGrubPrimary.copy(alpha = 0.2f),
    onPrimaryContainer = GameGrubForeground,

    secondary = GameGrubSecondary,
    onSecondary = GameGrubForeground,
    secondaryContainer = GameGrubSecondary.copy(alpha = 0.8f),
    onSecondaryContainer = GameGrubForeground,

    tertiary = GameGrubCyan,
    onTertiary = GameGrubForeground,
    tertiaryContainer = GameGrubCyan.copy(alpha = 0.2f),
    onTertiaryContainer = GameGrubForeground,

    background = GameGrubBackground,
    onBackground = GameGrubForeground,

    surface = GameGrubCard,
    onSurface = GameGrubForeground,
    surfaceVariant = GameGrubSecondary,
    onSurfaceVariant = GameGrubForegroundMuted,
    surfaceTint = GameGrubPrimary,

    inverseSurface = GameGrubForeground,
    inverseOnSurface = GameGrubBackground,
    inversePrimary = GameGrubPrimary,

    error = GameGrubDestructive,
    onError = GameGrubForeground,
    errorContainer = GameGrubDestructive.copy(alpha = 0.2f),
    onErrorContainer = GameGrubForeground,

    outline = GameGrubForegroundMuted,
    outlineVariant = GameGrubSecondary,

    scrim = Color.Black.copy(alpha = 0.5f),
    surfaceBright = GameGrubSecondary,
    surfaceDim = GameGrubBackground,
    surfaceContainer = GameGrubCard,
    surfaceContainerHigh = GameGrubSecondary,
    surfaceContainerHighest = GameGrubSecondary.copy(alpha = 0.9f),
    surfaceContainerLow = GameGrubBackground,
    surfaceContainerLowest = GameGrubBackground,
)

@Composable
fun GameGrubTheme(
    seedColor: Color = GameGrubSeed,
    isDark: Boolean = true, // for now, always force dark theme
    isAmoled: Boolean = false,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    val gameGrubColors = if (isDark) DarkGameGrubColors else DarkGameGrubColors // We can use LightGameGrubColors when ready

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
    }

    CompositionLocalProvider(LocalGameGrubColors provides gameGrubColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GameGrubTypography,
            content = content,
        )
    }
}

/**
 * Accessor for GameGrub custom colors.
 * Usage: GameGrubTheme.colors.accentCyan
 */
object GameGrubTheme {
    val colors: GameGrubColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGameGrubColors.current
}

/**
 * Direct access to dark colors for non-Composable contexts.
 * Prefer GameGrubTheme.colors when inside a Composable.
 */
object DarkColors {
    val statusInstalled = StatusInstalled
    val statusDownloading = StatusDownloading
    val statusAvailable = StatusAvailable
    val statusAway = StatusAway
    val statusOffline = StatusOffline

    val friendOnline = FriendOnline
    val friendOffline = FriendOffline
    val friendInGame = FriendInGame
    val friendAwayOrSnooze = FriendAwayOrSnooze
    val friendInGameAwayOrSnooze = FriendInGameAwayOrSnooze
    val friendBlocked = FriendBlocked

    val accentCyan = GameGrubCyan
    val accentPurple = GameGrubPurple
    val accentPink = GameGrubPink
    val accentSuccess = GameGrubSuccess
    val accentWarning = GameGrubWarning
    val accentDanger = GameGrubDanger

    val surfacePanel = GameGrubSurface
    val surfaceElevated = GameGrubSurfaceElevated

    val borderDefault = GameGrubBorder
    val textMuted = GameGrubForegroundMuted

    val compatibilityGood = CompatibilityGood
    val compatibilityGoodBackground = CompatibilityGoodBg
    val compatibilityPartial = CompatibilityPartial
    val compatibilityPartialBackground = CompatibilityPartialBg
    val compatibilityUnknown = CompatibilityUnknown
    val compatibilityUnknownBackground = CompatibilityUnknownBg
    val compatibilityBad = CompatibilityBad
    val compatibilityBadBackground = CompatibilityBadBg
}

// Settings tile color helpers
@Composable
fun settingsTileColors(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = GameGrubForeground,
    subtitleColor = GameGrubForegroundMuted,
    actionColor = GameGrubCyan,
)

@Composable
fun settingsTileColorsAlt(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = GameGrubForeground,
    subtitleColor = GameGrubForegroundMuted,
)

@Composable
fun settingsTileColorsDebug(): SettingsTileColors = SettingsTileDefaults.colors(
    titleColor = GameGrubDestructive,
    subtitleColor = GameGrubForegroundMuted,
    actionColor = GameGrubCyan,
)
