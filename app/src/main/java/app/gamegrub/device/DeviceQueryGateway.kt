package app.gamegrub.device


/**
 * Central boundary for all user-device and hardware queries.
 *
 * This gateway is the single contract that callers should use instead of directly touching
 * Android framework device APIs (`Build`, `Settings`, `BatteryManager`, `/proc`, `/sys`) or
 * Winlator GPU probes (`GPUInformation`).
 */
interface DeviceQueryGateway {
    /**
     * Build a snapshot containing identity-level device metadata.
     *
     * @return Immutable identity snapshot suitable for logs, telemetry, and diagnostics.
     */
    fun getIdentitySnapshot(): DeviceIdentitySnapshot

    /**
     * Resolve user-visible device name from system settings with safe fallback behavior.
     *
     * @return Friendly device name if available, otherwise machine name fallback.
     */
    fun getFriendlyDeviceName(): String

    /**
     * Resolve a stable hash used for device identity use-cases.
     *
     * @return Hashed `ANDROID_ID` as an `Int`.
     */
    fun getUniqueDeviceIdHash(): Int

    /**
     * Resolve current GPU renderer label.
     *
     * @return Renderer text, or `null` if unavailable.
     */
    fun getGpuRenderer(): String?

    /**
     * Resolve SOC identifier.
     *
     * @return SOC model when available, otherwise `null`.
     */
    fun getSocName(): String?

    /**
     * Determine if the current GPU is Turnip-capable.
     *
     * @return `true` when renderer matches supported Turnip families.
     */
    fun isTurnipCapable(): Boolean

    /**
     * Determine if the current GPU is an Adreno 6xx family renderer.
     *
     * @return `true` for Adreno 6xx-class devices.
     */
    fun isAdreno6xx(): Boolean

    /**
     * Determine if the current GPU is an Adreno 8 Elite class renderer.
     *
     * @return `true` when renderer indicates 83x/84x/85x class Adreno.
     */
    fun isAdreno8Elite(): Boolean

    /**
     * Determine if the current GPU matches Adreno 710/720/732 class renderers.
     *
     * @return `true` for 710/720/732 renderers.
     */
    fun isAdreno710720732(): Boolean

    /**
     * Resolve current GPU renderer as a non-null value for strict call sites.
     *
     * @return Renderer text, or `Unknown GPU` when unavailable.
     */
    fun getGpuRendererOrUnknown(): String

    /**
     * Resolve GPU device ID for a named renderer from shipped GPU metadata.
     *
     * @param gpuName GPU renderer name to match.
     * @return Device ID string, or empty string if not found.
     */
    fun getDeviceIdFromGpuName(gpuName: String): String

    /**
     * Resolve GPU vendor ID for a named renderer from shipped GPU metadata.
     *
     * @param gpuName GPU renderer name to match.
     * @return Vendor ID string, or empty string if not found.
     */
    fun getVendorIdFromGpuName(gpuName: String): String

    /**
     * Resolve renderer from active driver/native probe context.
     *
     * @return Renderer text from native probe, or `null` if unavailable.
     */
    fun getActiveDriverRenderer(): String?

    /**
     * Resolve vendor ID from active driver/native probe context.
     *
     * @return Vendor ID integer from native probe.
     */
    fun getActiveDriverVendorId(): Int


    /**
     * Read current CPU usage percentage.
     *
     * @return Usage percentage from `0..100`, or `null` when unavailable.
     */
    fun readCpuUsagePercent(): Int?

    /**
     * Read current GPU usage percentage.
     *
     * @return Usage percentage from `0..100`, or `null` when unavailable.
     */
    fun readGpuUsagePercent(): Int?

    /**
     * Read currently used RAM as a user-facing string.
     *
     * @return RAM usage text (for example `1.5GB` or `900MB`), or placeholder when unavailable.
     */
    fun readUsedRamText(): String

    /**
     * Read CPU temperature in celsius.
     *
     * @return Integer celsius value, or `null` when unavailable.
     */
    fun readCpuTempC(): Int?

    /**
     * Read GPU temperature in celsius.
     *
     * @return Integer celsius value, or `null` when unavailable.
     */
    fun readGpuTempC(): Int?

    /**
     * Read battery telemetry used by HUD and runtime overlays.
     *
     * @return Battery snapshot with percent/current/charge/voltage and charging state.
     */
    fun readBatterySnapshot(): DeviceBatterySnapshot

    /**
     * Read localized current time text for HUD rendering.
     *
     * @return Time text formatted for the current locale.
     */
    fun readClockText(): String
}

/**
 * Immutable device identity snapshot.
 *
 * @property manufacturer Device manufacturer.
 * @property brand Device brand.
 * @property model Device model.
 * @property machineName Combined human-readable machine name.
 * @property androidVersion Android release version string.
 * @property socName SOC model string when available.
 */
data class DeviceIdentitySnapshot(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val machineName: String,
    val androidVersion: String,
    val socName: String?,
)

/**
 * Raw battery telemetry snapshot.
 *
 * @property percent Battery percentage in `0..100` when available.
 * @property status Battery charging/discharging status.
 * @property currentMicroAmps Current draw in microamps (absolute value).
 * @property chargeCounterMicroAmpHours Charge counter in microamp-hours.
 * @property voltageMilliVolts Current battery voltage in millivolts.
 */
data class DeviceBatterySnapshot(
    val percent: Int? = null,
    val status: Int = -1,
    val currentMicroAmps: Long = 0L,
    val chargeCounterMicroAmpHours: Long = 0L,
    val voltageMilliVolts: Int = 0,
)

