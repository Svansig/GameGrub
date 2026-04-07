package app.gamegrub.domain.library.compatibility

import app.gamegrub.api.compatibility.GameCompatibilityService
import app.gamegrub.data.GameCompatibilityStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class CompatibilityStatusMapperTest {
    @Test
    fun toStatus_notWorking_returnsNotCompatible() {
        val response = response(isNotWorking = true)

        assertEquals(GameCompatibilityStatus.NOT_COMPATIBLE, CompatibilityStatusMapper.toStatus(response))
    }

    @Test
    fun toStatus_gpuPlayable_returnsGpuCompatible() {
        val response = response(hasBeenTried = true, gpuPlayableCount = 2)

        assertEquals(GameCompatibilityStatus.GPU_COMPATIBLE, CompatibilityStatusMapper.toStatus(response))
    }

    @Test
    fun toStatus_totalPlayable_returnsCompatible() {
        val response = response(hasBeenTried = true, totalPlayableCount = 3)

        assertEquals(GameCompatibilityStatus.COMPATIBLE, CompatibilityStatusMapper.toStatus(response))
    }

    @Test
    fun toStatus_notTried_returnsUnknown() {
        val response = response(hasBeenTried = false)

        assertEquals(GameCompatibilityStatus.UNKNOWN, CompatibilityStatusMapper.toStatus(response))
    }

    private fun response(
        hasBeenTried: Boolean = true,
        totalPlayableCount: Int = 0,
        gpuPlayableCount: Int = 0,
        isNotWorking: Boolean = false,
    ): GameCompatibilityService.GameCompatibilityResponse {
        return GameCompatibilityService.GameCompatibilityResponse(
            gameName = "Sample",
            totalPlayableCount = totalPlayableCount,
            gpuPlayableCount = gpuPlayableCount,
            avgRating = 0f,
            hasBeenTried = hasBeenTried,
            isNotWorking = isNotWorking,
        )
    }
}

