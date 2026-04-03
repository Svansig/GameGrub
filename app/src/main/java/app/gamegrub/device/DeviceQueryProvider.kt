package app.gamegrub.device

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Hilt entry-point contract used by framework/static call sites that cannot use constructor injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DeviceQueryEntryPoint {
    /**
     * Resolve the singleton device query gateway.
     *
     * @return App-scoped [DeviceQueryGateway] implementation.
     */
    fun deviceQueryGateway(): DeviceQueryGateway
}

/**
 * Provider helper for obtaining [DeviceQueryGateway] from non-injected contexts.
 */
object DeviceQueryProvider {
    /**
     * Resolve [DeviceQueryGateway] from an Android context via Hilt entry points.
     *
     * @param context Any Android context.
     * @return Singleton [DeviceQueryGateway].
     */
    fun from(context: Context): DeviceQueryGateway =
        EntryPointAccessors
            .fromApplication(context.applicationContext, DeviceQueryEntryPoint::class.java)
            .deviceQueryGateway()
}
