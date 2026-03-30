package app.gamegrub.service.steam.managers

/**
 * Routes Steam controller template indices to template source/type.
 */
object SteamControllerTemplateRoutingManager {
    enum class TemplateSource {
        Downloaded,
        Manifest,
        BuiltIn,
    }

    data class TemplateRoute(
        val source: TemplateSource,
        val builtInTemplateName: String? = null,
    )

    fun routeFor(templateIndex: Int): TemplateRoute {
        return when (templateIndex) {
            1 -> TemplateRoute(source = TemplateSource.Downloaded)
            13 -> TemplateRoute(source = TemplateSource.Manifest)
            2, 12 -> TemplateRoute(
                source = TemplateSource.BuiltIn,
                builtInTemplateName = "controller_xboxone_gamepad_fps.vdf",
            )

            6 -> TemplateRoute(
                source = TemplateSource.BuiltIn,
                builtInTemplateName = "controller_xboxone_wasd.vdf",
            )

            4, 5 -> TemplateRoute(
                source = TemplateSource.BuiltIn,
                builtInTemplateName = "gamepad_joystick.vdf",
            )

            else -> TemplateRoute(
                source = TemplateSource.BuiltIn,
                builtInTemplateName = "gamepad+mouse.vdf",
            )
        }
    }

    fun requiresWorkshopDownload(templateIndex: Int): Boolean {
        return routeFor(templateIndex).source == TemplateSource.Downloaded
    }
}

