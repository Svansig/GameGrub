package app.gamegrub.service.steam.managers

import app.gamegrub.device.DeviceQueryGateway
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Steam-facing device identity manager.
 *
 * This manager delegates device identity queries to [DeviceQueryGateway] so Steam identity
 * paths do not access Android framework identifiers directly.
 *
 * @property deviceQueryGateway Central device query gateway.
 */
@Singleton
class SteamDeviceIdentityManager @Inject constructor(
    private val deviceQueryGateway: DeviceQueryGateway,
) {
    /**
     * Resolve the machine/device name used by Steam identity logic.
     *
     * @return Friendly device name with safe fallback behavior.
     */
    fun getMachineName(): String = deviceQueryGateway.getFriendlyDeviceName()

    /**
     * Resolve unique device identifier hash used by Steam identity logic.
     *
     * @return Stable hash derived from `ANDROID_ID`.
     */
    fun getUniqueDeviceId(): Int = deviceQueryGateway.getUniqueDeviceIdHash()
}
