package app.gamegrub.ui.screen.xserver

import com.winlator.container.Container
import com.winlator.inputcontrols.ControlsProfile
import com.winlator.inputcontrols.InputControlsManager
import timber.log.Timber

/**
 * Resolves or creates input-control profiles associated with a container while
 * preserving the current fallback chain used by XServerScreen.
 */
internal object XServerControlsProfileResolver {
    fun resolveCurrentOrFallbackProfile(
        manager: InputControlsManager?,
        container: Container,
    ): ControlsProfile? {
        val currentManager = manager ?: return null
        val profiles = currentManager.getProfiles(false)
        if (profiles.isEmpty()) {
            return null
        }

        return resolveCustomProfile(currentManager, container)
            ?: currentManager.getProfile(0)
            ?: profiles.getOrNull(2)
            ?: profiles.first()
    }

    fun getOrCreateContainerProfile(
        manager: InputControlsManager,
        container: Container,
        gameName: String,
        suffix: String,
        onProfileCreated: ((ControlsProfile) -> Unit)? = null,
    ): ControlsProfile? {
        resolveCustomProfile(manager, container)?.let { return it }

        val sourceProfile = resolveDefaultSourceProfile(manager) ?: return null
        return runCatching {
            manager.duplicateProfile(sourceProfile).apply {
                name = "$gameName - $suffix"
                save()
                container.putExtra("profileId", id.toString())
                container.saveData()
                onProfileCreated?.invoke(this)
            }
        }.getOrElse { error ->
            Timber.e(error, "Failed to auto-create profile for container %s", container.name)
            sourceProfile
        }
    }

    private fun resolveCustomProfile(manager: InputControlsManager, container: Container): ControlsProfile? {
        val profileId = container.getExtra("profileId", "0").toIntOrNull() ?: 0
        return if (profileId != 0) {
            manager.getProfile(profileId)
        } else {
            null
        }
    }

    private fun resolveDefaultSourceProfile(manager: InputControlsManager): ControlsProfile? {
        val profiles = manager.getProfiles(false)
        return manager.getProfile(0)
            ?: profiles.firstOrNull { it.id == 2 }
            ?: profiles.firstOrNull()
    }
}

