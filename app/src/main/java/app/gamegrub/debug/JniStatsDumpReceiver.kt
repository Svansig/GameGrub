package app.gamegrub.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.gamegrub.BuildConfig
import app.gamegrub.ui.runtime.XServerRuntime
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xenvironment.components.VirGLRendererComponent
import timber.log.Timber

/**
 * Debug-only broadcast receiver used to dump current JNI instrumentation snapshots on demand.
 */
class JniStatsDumpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) {
            Timber.tag(TAG).w("Ignoring JNI stats dump request outside debug builds")
            return
        }

        val reset = intent.getBooleanExtra(EXTRA_RESET, false)
        val scenario = intent.getStringExtra(EXTRA_SCENARIO) ?: "unspecified"
        val environment = XServerRuntime.get().xEnvironment
        val virglStats = environment
            ?.getComponent(VirGLRendererComponent::class.java)
            ?.dumpPerfStats(reset)
            ?: "VirGL JNI perf: inactive"
        val xConnectorStats = XConnectorEpoll.getNativePerfStats(reset)

        Timber.tag(TAG).i(
            "[JniStatsDump][scenario=%s][reset=%s] %s | %s",
            scenario,
            reset,
            xConnectorStats,
            virglStats,
        )
    }

    companion object {
        const val ACTION_DUMP_JNI_STATS = "app.gamegrub.DEBUG_DUMP_JNI_STATS"
        const val EXTRA_RESET = "reset"
        const val EXTRA_SCENARIO = "scenario"
        private const val TAG = "JniStatsDumpReceiver"
    }
}

