package app.gamegrub.fallback

import app.gamegrub.launch.error.FailureClass
import app.gamegrub.telemetry.record.LaunchOutcome
import timber.log.Timber
import java.util.UUID

data class FallbackState(
    val sessionId: String,
    val currentLevel: FallbackLevel = FallbackLevel.NONE,
    val attempts: Int = 0,
    val maxAttempts: Int = 3,
    val history: List<FallbackTransition> = emptyList(),
    val isExhausted: Boolean = false,
)

data class FallbackTransition(
    val fromLevel: FallbackLevel,
    val toLevel: FallbackLevel,
    val failureClass: FailureClass,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String,
)

enum class FallbackLevel {
    NONE,
    CACHE_CLEAR,
    DRIVER_SWITCH,
    RUNTIME_SWITCH,
    PROFILE_SWITCH,
    CONTAINER_RESET,
    FINAL_RETRY,
    EXHAUSTED,
}

class FallbackStateMachine(
    private val maxAttemptsPerLevel: Int = 2,
) {
    private val states = mutableMapOf<String, FallbackState>()

    fun startFallback(sessionId: String): FallbackState {
        val state = FallbackState(sessionId = sessionId)
        states[sessionId] = state
        Timber.i("Started fallback state machine for session: $sessionId")
        return state
    }

    fun recordFailure(
        sessionId: String,
        failureClass: FailureClass,
    ): FallbackState {
        val current = states[sessionId] ?: startFallback(sessionId)

        if (current.isExhausted) {
            Timber.w("Fallback exhausted for session: $sessionId")
            return current
        }

        if (!FallbackFailureClass.canAutoFallback(failureClass)) {
            Timber.i("No automatic fallback available for: $failureClass")
            return current.copy(isExhausted = true)
        }

        val nextLevel = determineNextLevel(current, failureClass)
        val transition = FallbackTransition(
            fromLevel = current.currentLevel,
            toLevel = nextLevel,
            failureClass = failureClass,
            reason = FallbackFailureClass.getFallbackStrategy(failureClass).description,
        )

        val updatedState = current.copy(
            currentLevel = nextLevel,
            attempts = current.attempts + 1,
            history = current.history + transition,
            isExhausted = nextLevel == FallbackLevel.EXHAUSTED || current.attempts >= current.maxAttempts,
        )

        states[sessionId] = updatedState

        Timber.i("Fallback transition: ${transition.fromLevel} -> ${transition.toLevel} for $failureClass")

        return updatedState
    }

    fun recordSuccess(sessionId: String): FallbackState? {
        val current = states[sessionId] ?: return null
        Timber.i("Fallback succeeded at level: ${current.currentLevel}")
        states.remove(sessionId)
        return current
    }

    fun getState(sessionId: String): FallbackState? = states[sessionId]

    fun isActive(sessionId: String): Boolean {
        val state = states[sessionId] ?: return false
        return !state.isExhausted
    }

    fun getFallbackRecommendation(sessionId: String): FallbackStrategy? {
        val state = states[sessionId] ?: return null

        return when (state.currentLevel) {
            FallbackLevel.NONE -> null
            FallbackLevel.CACHE_CLEAR -> FallbackStrategy(
                action = app.gamegrub.launch.error.RecoveryAction.CACHE_INVALIDATION,
                description = "Clear shader cache and retry",
            )
            FallbackLevel.DRIVER_SWITCH -> FallbackStrategy(
                targetDriver = "mesa-24.0",
                action = app.gamegrub.launch.error.RecoveryAction.FALLBACK_DRIVER,
                description = "Switch to Mesa driver",
            )
            FallbackLevel.RUNTIME_SWITCH -> FallbackStrategy(
                targetRuntime = "wine-8.0-glibc2.35",
                action = app.gamegrub.launch.error.RecoveryAction.FALLBACK_RUNTIME,
                description = "Switch to stable Wine",
            )
            FallbackLevel.PROFILE_SWITCH -> FallbackStrategy(
                action = app.gamegrub.launch.error.RecoveryAction.FALLBACK_PROFILE,
                description = "Use different launch profile",
            )
            FallbackLevel.CONTAINER_RESET -> FallbackStrategy(
                action = app.gamegrub.launch.error.RecoveryAction.CONTAINER_RESET,
                description = "Reset container to defaults",
            )
            FallbackLevel.FINAL_RETRY -> FallbackStrategy(
                action = app.gamegrub.launch.error.RecoveryAction.RETRY_SAME_CONFIG,
                description = "Final retry with same config",
            )
            FallbackLevel.EXHAUSTED -> FallbackStrategy(
                action = app.gamegrub.launch.error.RecoveryAction.USER intervention_REQUIRED,
                description = "All fallback attempts exhausted",
            )
        }
    }

    private fun determineNextLevel(current: FallbackState, failureClass: FailureClass): FallbackLevel {
        if (current.currentLevel == FallbackLevel.NONE) {
            return when (failureClass) {
                FailureClass.CORRUPTED_CACHE -> FallbackLevel.CACHE_CLEAR
                FailureClass.GRAPHICS_INIT -> FallbackLevel.DRIVER_SWITCH
                FailureClass.MISSING_DRIVER -> FallbackLevel.DRIVER_SWITCH
                FailureClass.BACKEND_INIT -> FallbackLevel.RUNTIME_SWITCH
                FailureClass.CONTAINER_SETUP -> FallbackLevel.CONTAINER_RESET
                FailureClass.PROCESS_SPAWN -> FallbackLevel.RUNTIME_SWITCH
                FailureClass.TIMEOUT -> FallbackLevel.FINAL_RETRY
                FailureClass.UNKNOWN -> FallbackLevel.EXHAUSTED
            }
        }

        return minOf(
            FallbackLevel.entries[current.currentLevel.ordinal + 1],
            FallbackLevel.EXHAUSTED,
        )
    }

    fun clear(sessionId: String) {
        states.remove(sessionId)
    }

    fun clearAll() {
        states.clear()
    }
}