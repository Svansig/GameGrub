package app.gamegrub.ui.data

import app.gamegrub.ui.enums.AppOptionMenuType

data class AppMenuOption(
    val optionType: AppOptionMenuType,
    val onClick: () -> Unit,
)
