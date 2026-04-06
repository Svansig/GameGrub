package app.gamegrub.ui.screen

/**
 * Destinations for top level screens, excluding home screen destinations.
 */
sealed class GameGrubScreen(val route: String) {
    data object Onboarding : GameGrubScreen("onboarding")
    data object LoginUser : GameGrubScreen("login")
    data object Home : GameGrubScreen("home")
    data object XServer : GameGrubScreen("xserver")
    data object Settings : GameGrubScreen("settings")
    data object Chat : GameGrubScreen("chat/{id}") {
        fun route(id: Long) = "chat/$id"
        const val ARG_ID = "id"
    }
}
