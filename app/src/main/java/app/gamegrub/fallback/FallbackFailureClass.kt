package app.gamegrub.fallback

import app.gamegrub.launch.error.FailureClass
import app.gamegrub.launch.error.RecoveryAction

/**
 * Priority levels for fallback attempts.
 *
 * Used to order fallback strategies based on likelihood of success
 * and severity of the failure.
 */
enum class FallbackPriority {
    IMMEDIATE,
    HIGH,
    MEDIUM,
    LOW,
    LAST_RESORT,
}

/**
 * A fallback strategy for handling a specific failure class.
 *
 * @property targetRuntime Alternative runtime to try (optional)
 * @property targetDriver Alternative driver to try (optional)
 * @property targetProfile Alternative profile to try (optional)
 * @property action The recovery action to take
 * @property description Human-readable explanation
 */
data class FallbackStrategy(
    val targetRuntime: String? = null,
    val targetDriver: String? = null,
    val targetProfile: String? = null,
    val action: RecoveryAction,
    val description: String,
)

/**
 * Maps launch failure classes to fallback strategies.
 *
 * Provides priority ordering and concrete fallback actions for each
 * type of launch failure defined in the taxonomy.
 *
 * @see FailureClass
 * @see FallbackStateMachine
 */
object FallbackFailureClass {
    fun getPriority(failureClass: FailureClass): FallbackPriority {
        return when (failureClass) {
            FailureClass.MISSING_DRIVER -> FallbackPriority.IMMEDIATE
            FailureClass.CORRUPTED_CACHE -> FallbackPriority.HIGH
            FailureClass.GRAPHICS_INIT -> FallbackPriority.HIGH
            FailureClass.BACKEND_INIT -> FallbackPriority.MEDIUM
            FailureClass.CONTAINER_SETUP -> FallbackPriority.MEDIUM
            FailureClass.PROCESS_SPAWN -> FallbackPriority.MEDIUM
            FailureClass.TIMEOUT -> FallbackPriority.LOW
            FailureClass.UNKNOWN -> FallbackPriority.LAST_RESORT
        }
    }

    fun getFallbackStrategy(failureClass: FailureClass): FallbackStrategy {
        return when (failureClass) {
            FailureClass.MISSING_DRIVER -> FallbackStrategy(
                targetDriver = "mesa-24.0",
                action = RecoveryAction.FALLBACK_DRIVER,
                description = "Fallback to Mesa driver",
            )
            FailureClass.CORRUPTED_CACHE -> FallbackStrategy(
                action = RecoveryAction.CACHE_INVALIDATION,
                description = "Clear cache and retry",
            )
            FailureClass.GRAPHICS_INIT -> FallbackStrategy(
                targetDriver = "mesa-24.0",
                action = RecoveryAction.FALLBACK_DRIVER,
                description = "Try Mesa instead of Turnip",
            )
            FailureClass.BACKEND_INIT -> FallbackStrategy(
                targetRuntime = "wine-8.0-glibc2.35",
                action = RecoveryAction.FALLBACK_RUNTIME,
                description = "Try stable Wine instead",
            )
            FailureClass.CONTAINER_SETUP -> FallbackStrategy(
                action = RecoveryAction.CONTAINER_RESET,
                description = "Reset container to defaults",
            )
            FailureClass.PROCESS_SPAWN -> FallbackStrategy(
                action = RecoveryAction.RE_EXTRACT,
                description = "Re-extract runtime",
            )
            FailureClass.TIMEOUT -> FallbackStrategy(
                action = RecoveryAction.RETRY_SAME_CONFIG,
                description = "Retry with same config",
            )
            FailureClass.UNKNOWN -> FallbackStrategy(
                action = RecoveryAction.NONE,
                description = "No automatic fallback available",
            )
        }
    }

    fun canAutoFallback(failureClass: FailureClass): Boolean {
        val strategy = getFallbackStrategy(failureClass)
        return strategy.action != RecoveryAction.NONE && strategy.action != RecoveryAction.USER_INTERVENTION_REQUIRED
    }
}