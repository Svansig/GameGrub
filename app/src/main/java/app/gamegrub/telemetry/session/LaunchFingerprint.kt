package app.gamegrub.telemetry.session

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Structured context for a single game launch attempt.
 *
 * Captures all relevant configuration and environment details needed for
 * telemetry, debugging, and recommendation systems. Emitted at launch time
 * and carries through the entire launch pipeline.
 *
 * @property sessionId Unique identifier for this launch session
 * @property timestamp Unix epoch milliseconds when the fingerprint was created
 * @property baseBundleId The base bundle (Linux userspace) identifier
 * @property baseBundleVersion Version string of the base bundle
 * @property runtimeId The runtime (Wine/Proton) identifier
 * @property runtimeVersion Version string of the runtime
 * @property driverId The graphics driver identifier
 * @property driverVersion Version string of the driver
 * @property containerId The container identifier
 * @property containerPath Path to the container root
 * @property gameTitle Display name of the game
 * @property gamePlatform Platform/source (steam, gog, epic, amazon)
 * @property deviceClass Device classification (e.g., "SM8550")
 * @property wineVersion Legacy Wine version field (for compatibility)
 * @property dxwrapper DXVK/VKD3D wrapper selection
 * @property containerVariant glibc or bionic variant
 */
@Serializable
data class LaunchFingerprint(
    val sessionId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val baseBundleId: String? = null,
    val baseBundleVersion: String? = null,
    val runtimeId: String? = null,
    val runtimeVersion: String? = null,
    val driverId: String? = null,
    val driverVersion: String? = null,
    val containerId: String? = null,
    val containerPath: String? = null,
    val gameTitle: String? = null,
    val gamePlatform: String? = null,
    val deviceClass: String? = null,
    val wineVersion: String? = null,
    val dxwrapper: String? = null,
    val containerVariant: String? = null,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): LaunchFingerprint = Json.decodeFromString(json)
    }

    fun logAtMilestone(milestone: String) {
        Timber.d(
            "[Fingerprint][%s] session=%s runtime=%s driver=%s container=%s game=%s",
            milestone, sessionId, runtimeId, driverId, containerId, gameTitle,
        )
    }
}

/**
 * Emitter for launch fingerprints.
 *
 * Provides thread-safe storage and retrieval of recent launch fingerprints.
 * Maintains an in-memory buffer of the last 100 fingerprints for quick access
 * during debugging and telemetry. Persisted separately via LaunchRecordStore.
 */
object LaunchFingerprintEmitter {
    private val json = Json { prettyPrint = true }
    private val fingerprints = java.util.concurrent.ConcurrentLinkedQueue<LaunchFingerprint>()
    private const val MAX_CACHED = 100

    fun emit(fingerprint: LaunchFingerprint) {
        fingerprints.offer(fingerprint)
        while (fingerprints.size > MAX_CACHED) {
            fingerprints.poll()
        }
        Timber.d("Fingerprint emitted: session=%s", fingerprint.sessionId)
        fingerprint.logAtMilestone("EMITTED")
    }

    fun getRecent(limit: Int = 10): List<LaunchFingerprint> {
        return fingerprints.toList().takeLast(limit).reversed()
    }

    fun getBySessionId(sessionId: String): LaunchFingerprint? {
        return fingerprints.find { it.sessionId == sessionId }
    }

    fun clear() {
        fingerprints.clear()
    }
}
