package app.gamegrub.telemetry.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID

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
        Timber.d("[Fingerprint][%s] session=%s runtime=%s driver=%s container=%s game=%s",
            milestone, sessionId, runtimeId, driverId, containerId, gameTitle)
    }
}

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