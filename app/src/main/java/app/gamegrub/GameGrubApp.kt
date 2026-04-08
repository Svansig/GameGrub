package app.gamegrub

// Add PostHog imports

import android.os.StrictMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.GOGGameDao
import app.gamegrub.events.EventDispatcher
import app.gamegrub.service.auth.PlayIntegrity
import app.gamegrub.service.steam.AchievementWatcher
import app.gamegrub.startup.StartupCoordinator
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.posthog.PersonProfiles
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.winlator.container.Container
import com.winlator.inputcontrols.InputControlsManager
import com.winlator.widget.InputControlsView
import com.winlator.widget.TouchpadView
import com.winlator.widget.XServerView
import com.winlator.xenvironment.XEnvironment
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

typealias NavChangedListener = NavController.OnDestinationChangedListener

@HiltAndroidApp
class GameGrubApp : SplitCompatApplication() {

    @Inject
    lateinit var gogGameDao: GOGGameDao

    @Inject
    lateinit var amazonGameDao: AmazonGameDao

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Allows to find resource streams not closed within GameGrub and JavaSteam
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )

            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // Delegate startup initialization to coordinator for testability
        StartupCoordinator().initialize(this)

        // Initialize PostHog Analytics
        val postHogConfig = PostHogAndroidConfig(
            apiKey = BuildConfig.POSTHOG_API_KEY,
            host = BuildConfig.POSTHOG_HOST,
        ).apply {
            /* turn every event into an identified one */
            personProfiles = PersonProfiles.ALWAYS
        }
        PostHogAndroid.setup(this, postHogConfig)

        PlayIntegrity.warmUp(this)
    }

    companion object {
        @JvmField
        @Deprecated("Use XServerRuntime.get().events instead", ReplaceWith("XServerRuntime.get().events"))
        val events: EventDispatcher = EventDispatcher()

        @Deprecated("Use XServerRuntime.get() instead")
        internal var onDestinationChangedListener: NavChangedListener? = null

        // TODO: find a way to make this saveable, this is terrible (leak that memory baby)
        @Deprecated("Use XServerRuntime.get().xEnvironment instead")
        internal var xEnvironment: XEnvironment? = null

        @Deprecated("Use XServerRuntime.get().xServerView instead")
        internal var xServerView: XServerView? = null

        @Deprecated("Use XServerRuntime.get().inputControlsView instead")
        var inputControlsView: InputControlsView? = null

        @Deprecated("Use XServerRuntime.get().inputControlsManager instead")
        var inputControlsManager: InputControlsManager? = null

        @Deprecated("Use XServerRuntime.get().touchpadView instead")
        var touchpadView: TouchpadView? = null

        @Deprecated("Use XServerRuntime.get().achievementWatcher instead")
        var achievementWatcher: AchievementWatcher? = null

        @Deprecated("Use XServerRuntime.get().isOverlayPaused instead")
        var isOverlayPaused by mutableStateOf(false)

        @Volatile
        @Deprecated("Use XServerRuntime.get().isActivityInForeground instead")
        var isActivityInForeground: Boolean = true

        // Active runtime suspend policy for the current in-game session.
        @Deprecated("Use XServerRuntime.get().activeSuspendPolicy instead")
        var activeSuspendPolicy: String = Container.SUSPEND_POLICY_MANUAL
            private set
        private var hasInitializedSuspendPolicyState: Boolean = false

        @Deprecated("Use XServerRuntime.get().setActiveSuspendPolicy instead")
        fun setActiveSuspendPolicy(policy: String) {
            activeSuspendPolicy = Container.normalizeSuspendPolicy(policy)
            hasInitializedSuspendPolicyState = true
        }

        @Deprecated("Use XServerRuntime.get().clearActiveSuspendState instead")
        fun clearActiveSuspendState() {
            activeSuspendPolicy = Container.SUSPEND_POLICY_MANUAL
            isOverlayPaused = false
            hasInitializedSuspendPolicyState = false
        }

        @Deprecated("Use XServerRuntime.get().hasValidSuspendPolicyState instead")
        fun hasValidSuspendPolicyState(): Boolean = hasInitializedSuspendPolicyState

        @Deprecated("Use XServerRuntime.get().isNeverSuspendMode instead")
        fun isNeverSuspendMode(): Boolean = activeSuspendPolicy.equals(Container.SUSPEND_POLICY_NEVER, ignoreCase = true)

        @Deprecated("Use XServerRuntime.get().isManualSuspendMode instead")
        fun isManualSuspendMode(): Boolean = activeSuspendPolicy.equals(Container.SUSPEND_POLICY_MANUAL, ignoreCase = true)
    }
}
