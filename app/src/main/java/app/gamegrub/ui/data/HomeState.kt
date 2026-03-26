package app.gamegrub.ui.data

import app.gamegrub.PrefManager
import app.gamegrub.ui.enums.HomeDestination

data class HomeState(
    val currentDestination: HomeDestination = PrefManager.startScreen,
    val confirmExit: Boolean = false,
)
