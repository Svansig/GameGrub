package app.gamegrub.domain.library.compatibility

import app.gamegrub.api.compatibility.GameCompatibilityService
import app.gamegrub.data.GameCompatibilityStatus

/**
 * Maps compatibility API payloads into library-facing compatibility statuses.
 */
object CompatibilityStatusMapper {
    fun toStatus(response: GameCompatibilityService.GameCompatibilityResponse): GameCompatibilityStatus {
        return when {
            response.isNotWorking -> GameCompatibilityStatus.NOT_COMPATIBLE
            !response.hasBeenTried -> GameCompatibilityStatus.UNKNOWN
            response.gpuPlayableCount > 0 -> GameCompatibilityStatus.GPU_COMPATIBLE
            response.totalPlayableCount > 0 -> GameCompatibilityStatus.COMPATIBLE
            else -> GameCompatibilityStatus.UNKNOWN
        }
    }
}

