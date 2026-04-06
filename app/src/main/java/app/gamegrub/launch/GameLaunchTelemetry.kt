package app.gamegrub.launch

import app.gamegrub.PrefManager
import app.gamegrub.utils.container.ContainerUtils
import com.posthog.PostHog

/**
 * Sends launch analytics for a successfully accepted game launch request.
 */
fun trackGameLaunched(appId: String) {
    val gameSource = ContainerUtils.extractGameSourceFromContainerId(appId)
    val gameName = ContainerUtils.resolveGameName(appId)
    PostHog.capture(
        event = "game_launched",
        properties = mapOf(
            "game_name" to gameName,
            "game_store" to gameSource.name,
            "key_attestation_available" to PrefManager.keyAttestationAvailable,
            "play_integrity_available" to PrefManager.playIntegrityAvailable,
        ),
    )
}
