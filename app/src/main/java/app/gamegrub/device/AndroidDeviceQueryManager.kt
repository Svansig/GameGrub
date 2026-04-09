package app.gamegrub.device

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import com.winlator.core.GPUInformation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Android-backed implementation of [DeviceQueryGateway].
 *
 * This manager is the canonical owner for device/hardware reads in the app and is designed
 * for Android 13+ runtime behavior while keeping conservative fallback paths for missing data.
 *
 * @property appContext Application context used for framework-backed hardware queries.
 */
@Singleton
class AndroidDeviceQueryManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : DeviceQueryGateway {
    /**
     * Previous `/proc/stat` total sample used for CPU usage delta calculation.
     */
    private var lastCpuTotal: Long? = null

    /**
     * Previous `/proc/stat` idle sample used for CPU usage delta calculation.
     */
    private var lastCpuIdle: Long? = null

    /**
     * Build immutable identity snapshot for telemetry and diagnostics.
     *
     * @return Device identity snapshot.
     */
    override fun getIdentitySnapshot(): DeviceIdentitySnapshot = DeviceIdentitySnapshot(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        model = Build.MODEL,
        machineName = getMachineName(),
        androidVersion = Build.VERSION.RELEASE,
        socName = getSocName(),
    )

    /**
     * Resolve a user-facing device name from settings with fallback behavior.
     *
     * @return Friendly device name or fallback machine name.
     */
    override fun getFriendlyDeviceName(): String {
        return try {
            Settings.Global.getString(appContext.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Settings.System.getString(appContext.contentResolver, "device_name")
                ?: getMachineName()
        } catch (_: Exception) {
            getMachineName()
        }
    }

    /**
     * Resolve a stable hash of `ANDROID_ID` for device identity use-cases.
     *
     * @return Hash of `ANDROID_ID`.
     */
    @SuppressLint("HardwareIds")
    override fun getUniqueDeviceIdHash(): Int {
        val androidId = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        return androidId.hashCode()
    }

    /**
     * Resolve current GPU renderer from cached/probed `GPUInformation`.
     *
     * @return Renderer text or `null` when unavailable.
     */
    override fun getGpuRenderer(): String? = GPUInformation.getRenderer(appContext)

    /**
     * Resolve SOC model string where available.
     *
     * @return SOC model on supported platform versions, otherwise `null`.
     */
    override fun getSocName(): String? = Build.SOC_MODEL.takeIf { it.isNotEmpty() }

    /**
     * Check whether renderer belongs to Turnip-capable families.
     *
     * @return `true` when Turnip is supported.
     */
    override fun isTurnipCapable(): Boolean = GPUInformation.isTurnipCapable(appContext)

    /**
     * Check whether renderer belongs to Adreno 6xx family.
     *
     * @return `true` for Adreno 6xx.
     */
    override fun isAdreno6xx(): Boolean = GPUInformation.isAdreno6xx(appContext)

    /**
     * Check whether renderer belongs to Adreno 8 Elite class.
     *
     * @return `true` for Adreno 8 Elite.
     */
    override fun isAdreno8Elite(): Boolean = GPUInformation.isAdreno8Elite(appContext)

    /**
     * Check whether renderer matches Adreno 710/720/732.
     *
     * @return `true` for Adreno 710/720/732.
     */
    override fun isAdreno710720732(): Boolean = GPUInformation.isAdreno710_720_732(appContext)

    /**
     * Resolve GPU renderer and always provide a non-null fallback.
     *
     * @return Renderer text, or `Unknown GPU` when unavailable.
     */
    override fun getGpuRendererOrUnknown(): String = getGpuRenderer().orEmpty().ifEmpty { "Unknown GPU" }

    /**
     * Resolve device ID for the provided GPU name.
     *
     * @param gpuName GPU name to match in bundled GPU metadata.
     * @return Device ID value or empty string.
     */
    override fun getDeviceIdFromGpuName(gpuName: String): String =
        GPUInformation.getDeviceIdFromGPUName(appContext, gpuName)

    /**
     * Resolve vendor ID for the provided GPU name.
     *
     * @param gpuName GPU name to match in bundled GPU metadata.
     * @return Vendor ID value or empty string.
     */
    override fun getVendorIdFromGpuName(gpuName: String): String =
        GPUInformation.getVendorIdFromGPUName(appContext, gpuName)

    /**
     * Resolve active native renderer from current driver context.
     *
     * @return Active renderer value or `null`.
     */
    override fun getActiveDriverRenderer(): String? = GPUInformation.getRenderer(null, null)

    /**
     * Resolve active native vendor ID from current driver context.
     *
     * @return Active vendor ID.
     */
    override fun getActiveDriverVendorId(): Int = GPUInformation.getVendorID(null, null)

    /**
     * Read CPU usage percent from `/proc/stat` using delta samples.
     *
     * @return CPU usage percent or `null` when unavailable.
     */
    override fun readCpuUsagePercent(): Int? {
        val parts = readFirstLine("/proc/stat")
            ?.trim()
            ?.split(Regex("\\s+"))
            ?: return null

        if (parts.size < 5 || parts.firstOrNull() != "cpu") {
            return null
        }

        val values = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (values.size < 4) {
            return null
        }

        val idle = values.getOrElse(3) { 0L }
        val iowait = values.getOrElse(4) { 0L }
        val total = values.sum()
        val idleTotal = idle + iowait

        val previousTotal = lastCpuTotal
        val previousIdle = lastCpuIdle
        lastCpuTotal = total
        lastCpuIdle = idleTotal

        if (previousTotal == null || previousIdle == null) {
            return null
        }

        val totalDiff = total - previousTotal
        val idleDiff = idleTotal - previousIdle
        if (totalDiff <= 0) {
            return null
        }

        return (((totalDiff - idleDiff).coerceAtLeast(0L)) * 100L / totalDiff).toInt().coerceIn(0, 100)
    }

    /**
     * Read GPU usage percent from KGSL sysfs.
     *
     * @return GPU usage percent or `null` when unavailable.
     */
    override fun readGpuUsagePercent(): Int? {
        val raw = readFirstLine("/sys/class/kgsl/kgsl-3d0/gpubusy") ?: return null
        val parts = raw.trim().split(Regex("\\s+"))
        if (parts.size < 2) {
            return null
        }

        val busy = parts[0].toLongOrNull() ?: return null
        val total = parts[1].toLongOrNull() ?: return null
        if (total <= 0L) {
            return null
        }

        return ((busy * 100L) / total).toInt().coerceIn(0, 100)
    }

    /**
     * Read used RAM from [ActivityManager].
     *
     * @return RAM text (`GB` or `MB`) or placeholder when unavailable.
     */
    override fun readUsedRamText(): String {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return "-"
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        val usedBytes = (info.totalMem - info.availMem).coerceAtLeast(0L)
        val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        return if (usedGb >= 1.0) {
            String.format(Locale.US, "%.1fGB", usedGb)
        } else {
            val usedMb = usedBytes / (1024L * 1024L)
            "${usedMb}MB"
        }
    }

    /**
     * Read CPU temperature from known thermal zone sources.
     *
     * @return Celsius value or `null`.
     */
    override fun readCpuTempC(): Int? {
        return readTemperatureC(
            discoverThermalZoneTempPaths { type ->
                type.contains("cpu") || type.contains("tsens")
            },
        )
    }

    /**
     * Read GPU temperature from known thermal/KGSL sources.
     *
     * @return Celsius value or `null`.
     */
    override fun readGpuTempC(): Int? {
        return readTemperatureC(
            listOf("/sys/class/kgsl/kgsl-3d0/temp") +
                discoverThermalZoneTempPaths { type ->
                    type.contains("gpu") || type.contains("kgsl")
                },
        )
    }

    /**
     * Read battery telemetry snapshot from [BatteryManager] and battery broadcast.
     *
     * @return Battery telemetry snapshot.
     */
    override fun readBatterySnapshot(): DeviceBatterySnapshot {
        val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return DeviceBatterySnapshot()

        val percent = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .takeIf { it in 0..100 }

        val statusIntent: Intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return DeviceBatterySnapshot(percent = percent)

        val status = statusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val currentMicroAmps = abs(batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
        val chargeCounterMicroAmpHours = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val voltageMilliVolts = statusIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)

        return DeviceBatterySnapshot(
            percent = percent,
            status = status,
            currentMicroAmps = currentMicroAmps,
            chargeCounterMicroAmpHours = chargeCounterMicroAmpHours,
            voltageMilliVolts = voltageMilliVolts,
        )
    }

    /**
     * Read localized clock text.
     *
     * @return Locale-formatted time string.
     */
    override fun readClockText(): String = DateFormat.getTimeFormat(appContext).format(Date())

    /**
     * Build a human-readable machine name from manufacturer and model.
     *
     * @return Machine label.
     */
    private fun getMachineName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    /**
     * Discover thermal zone temp file paths matching a caller predicate.
     *
     * @param matches Predicate used against thermal zone type strings.
     * @return Matching thermal zone temp file paths.
     */
    private fun discoverThermalZoneTempPaths(matches: (String) -> Boolean): List<String> {
        val thermalDir = File("/sys/class/thermal")
        val zones = thermalDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("thermal_zone")
        } ?: return emptyList()

        return zones.mapNotNull { zone ->
            val type = readFirstLine(File(zone, "type").path)?.trim()?.lowercase(Locale.US) ?: return@mapNotNull null
            if (!matches(type)) {
                return@mapNotNull null
            }
            File(zone, "temp").path
        }
    }

    /**
     * Read and normalize temperature from provided raw file paths.
     *
     * @param paths Candidate temperature file paths.
     * @return First valid celsius reading or `null`.
     */
    private fun readTemperatureC(paths: List<String>): Int? {
        for (path in paths.distinct()) {
            val raw = readFirstLine(path)?.trim()?.toIntOrNull() ?: continue
            val celsius = if (raw > 1000) raw / 1000 else raw
            if (celsius in 1..150) {
                return celsius
            }
        }
        return null
    }

    /**
     * Read first line from a file path.
     *
     * @param path Absolute file path.
     * @return First line text or `null`.
     */
    private fun readFirstLine(path: String): String? {
        return try {
            File(path).bufferedReader().use { it.readLine() }
        } catch (_: Exception) {
            null
        }
    }
}
