package app.gamegrub.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.Serializable
import timber.log.Timber

/**
 * Storage policy for game data placement.
 *
 * Defines where different types of game data (runtime, cache, installs)
 * should be stored. Supports hot (internal), cold (external), and
 * hybrid placement strategies.
 *
 * @property location Primary storage location
 */
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
        override val location: StorageLocation = StorageLocation.AUTO,
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

    fun isLocationAvailable(location: StorageLocation, context: Context? = null): Boolean {
        return when (location) {
            StorageLocation.INTERNAL -> {
                try {
                    val internalDir = context?.filesDir ?: Environment.getDataDirectory()
                    val stat = StatFs(internalDir.path)
                    stat.availableBlocks * stat.blockSize > MIN_SPACE_BYTES
                } catch (e: Exception) {
                    Timber.e(e, "Failed to check internal storage availability")
                    false
                }
            }

            StorageLocation.EXTERNAL -> {
                val state = Environment.getExternalStorageState()
                if (state != Environment.MEDIA_MOUNTED) {
                    Timber.d("External storage not mounted: $state")
                    return false
                }

                try {
                    val externalDir = context?.getExternalFilesDir(null) ?: return false
                    if (!externalDir.exists()) {
                        return false
                    }
                    val stat = StatFs(externalDir.path)
                    stat.availableBlocks * stat.blockSize > MIN_SPACE_BYTES
                } catch (e: Exception) {
                    Timber.e(e, "Failed to check external storage availability")
                    false
                }
            }

            StorageLocation.AUTO -> {
                isLocationAvailable(StorageLocation.INTERNAL, context) ||
                    isLocationAvailable(StorageLocation.EXTERNAL, context)
            }
        }
    }

    private const val MIN_SPACE_BYTES = 256L * 1024 * 1024

    fun getRecommendedPolicy(): StoragePolicy = StoragePolicy.DEFAULT
}
