package app.gamegrub.container.launch.command

/**
 * Strategy for building a launch command for one game store/source.
 *
 * Return null when this builder does not handle the provided source.
 */
internal interface StoreLaunchCommandBuilder {
    fun build(context: LaunchCommandContext): String?
}
