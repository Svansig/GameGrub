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

//    @Inject
//    lateinit var gogGameDao: GOGGameDao
//
//    @Inject
//    lateinit var amazonGameDao: AmazonGameDao

//    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // Initialize PostHog Analytics - gated by privacy consent
        if (PrefManager.analyticsConsent) {
            val postHogConfig = PostHogAndroidConfig(
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST,
            ).apply {
                /* turn every event into an identified one */
                personProfiles = PersonProfiles.ALWAYS
            }
            PostHogAndroid.setup(this, postHogConfig)
            Timber.i("PostHog analytics initialized (consent granted)")
        } else {
            Timber.i("PostHog analytics not initialized (no consent)")
        }

        PlayIntegrity.warmUp(this)
    }

    companion object {
        @Volatile
        @Deprecated("Use XServerRuntime.get().isActivityInForeground instead")
        var isActivityInForeground: Boolean = true
    }
}
