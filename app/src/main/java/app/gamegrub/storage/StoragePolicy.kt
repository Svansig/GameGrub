package app.gamegrub.storage

import kotlinx.serialization.Serializable

@Serializable
sealed class StoragePolicy {
    abstract val location: StorageLocation

    @Serializable
    data class HotRuntime(
        override val location: StorageLocation = StorageLocation.INTERNAL,
    ) : StoragePolicy() {
        companion object {
            val INSTANCE = HotRuntime()
        }
    }

    @Serializable
    data class ColdBulk(
        override val location: StorageLocation = StorageLocation.EXTERNAL,
        val minFreeSpace: Long = 1024L * 1024 * 1024,
    ) : StoragePolicy()

    @Serializable
    data class Hybrid(
        val prefixLocation: StorageLocation = StorageLocation.INTERNAL,
        val cacheLocation: StorageLocation = StorageLocation.INTERNAL,
        val installLocation: StorageLocation = StorageLocation.EXTERNAL,
    ) : StoragePolicy()

    companion object {
        val DEFAULT = HotRuntime()
    }
}

@Serializable
enum class StorageLocation {
    INTERNAL,
    EXTERNAL,
    AUTO,
}

@Serializable
data class StorageRequirement(
    val minSpaceBytes: Long,
    val preferredSpaceBytes: Long = minSpaceBytes * 2,
    val warnThresholdBytes: Long = minSpaceBytes / 2,
)

object StoragePolicyHelper {
    fun getRequirementForPolicy(policy: StoragePolicy): StorageRequirement {
        return when (policy) {
            is StoragePolicy.HotRuntime -> StorageRequirement(
                minSpaceBytes = 512L * 1024 * 1024,
                preferredSpaceBytes = 1024L * 1024 * 1024,
            )
            is StoragePolicy.ColdBulk -> policy.minFreeSpace.let {
                StorageRequirement(minSpaceBytes = it)
            }
            is StoragePolicy.Hybrid -> StorageRequirement(
                minSpaceBytes = 256L * 1024 * 1024,
                preferredSpaceBytes = 512L * 1024 * 1024,
            )
        }
    }

    fun isLocationAvailable(location: StorageLocation): Boolean {
        return location == StorageLocation.INTERNAL
    }

    fun getRecommendedPolicy(): StoragePolicy = StoragePolicy.DEFAULT
}