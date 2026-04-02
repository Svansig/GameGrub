package app.gamegrub

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color.TRANSPARENT
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.ServiceLifecycleManager
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.GameGrubMain
import app.gamegrub.ui.ImmersiveModeManager
import app.gamegrub.ui.OrientationManager
import app.gamegrub.ui.orientation.OrientationPolicy
import app.gamegrub.ui.utils.LocaleHelper
import app.gamegrub.utils.container.ContainerUtils
import coil.ImageLoader
import com.posthog.PostHog
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import com.winlator.core.AppUtils
import com.winlator.inputcontrols.ControllerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var imageLoader: ImageLoader

    private lateinit var immersiveModeManager: ImmersiveModeManager
    private lateinit var orientationManager: OrientationManager

    private val onSetSystemUi: (AndroidEvent.SetSystemUIVisibility) -> Unit = {
        immersiveModeManager.setSystemUIVisibility(it.visible)
    }

    private val onSetOrientationPolicy: (AndroidEvent.SetOrientationPolicy) -> Unit = {
        orientationManager.setOrientationPolicy(it.policy)
    }

    private val onEndProcess: (AndroidEvent.EndProcess) -> Unit = {
        finishAndRemoveTask()
    }

    override fun attachBaseContext(newBase: Context) {
        PrefManager.init(newBase)
        val languageCode = PrefManager.appLanguage
        val context = LocaleHelper.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        immersiveModeManager = ImmersiveModeManager(window)
        orientationManager = OrientationManager(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        immersiveModeManager.applyImmersiveMode()
        orientationManager.setOrientationPolicy(OrientationPolicy.default(PrefManager.allowedOrientation))

        ControllerManager.getInstance().init(applicationContext)
        ContainerUtils.setContainerDefaults(applicationContext)

        handleLaunchIntent(intent)

        AppUtils.keepScreenOn(this)

        GameGrubApp.events.on<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        GameGrubApp.events.on<AndroidEvent.SetOrientationPolicy, Unit>(onSetOrientationPolicy)
        GameGrubApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

        setContent {
            val shouldRequestNotificationPermission: Boolean = remember {
                app.gamegrub.ui.utils.NotificationPermissionGate.shouldRequestNotificationPermission(this)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) {
                // The prompted flag prevents repeated startup prompts after deny/ignore.
            }

            LaunchedEffect(shouldRequestNotificationPermission) {
                if (shouldRequestNotificationPermission) {
                    app.gamegrub.ui.utils.NotificationPermissionGate.markPrompted()
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                GameGrubMain()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent, isNewIntent = true)
    }

    private fun handleLaunchIntent(intent: Intent, isNewIntent: Boolean = false) {
        LaunchRequestManager.handleLaunchIntent(this, intent, isNewIntent)
    }

    override fun onDestroy() {
        GameGrubApp.events.emit(AndroidEvent.ActivityDestroyed)

        GameGrubApp.events.off<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        GameGrubApp.events.off<AndroidEvent.SetOrientationPolicy, Unit>(onSetOrientationPolicy)
        GameGrubApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)

        ServiceLifecycleManager.onDestroy(isChangingConfigurations)
        super.onDestroy()
    }

    private fun hasReadyGameLifecycleState(action: String): Boolean {
        if (!SteamService.keepAlive) return false
        if (!GameGrubApp.hasValidSuspendPolicyState()) {
            Timber.d("Skipping game %s because suspend policy state is not initialized", action)
            return false
        }
        if (GameGrubApp.xEnvironment == null) {
            Timber.d("Skipping game %s because xEnvironment is not ready", action)
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        GameGrubApp.isActivityInForeground = true
        immersiveModeManager.applyImmersiveMode()

        SteamService.autoStopWhenIdle = false

        if (hasReadyGameLifecycleState("resume")) {
            when {
                GameGrubApp.isNeverSuspendMode() -> {
                    Timber.d("Game resume skipped due to suspend policy=never")
                }

                GameGrubApp.isOverlayPaused -> {
                    if (GameGrubApp.isManualSuspendMode()) {
                        Timber.d("Game remains suspended until user presses Resume")
                    }
                }

                else -> {
                    GameGrubApp.xEnvironment?.onResume()
                    Timber.d("Game resumed")
                }
            }
        }

        ServiceLifecycleManager.onResume(this)

        PostHog.capture(event = "app_foregrounded")
    }

    override fun onPause() {
        GameGrubApp.isActivityInForeground = false
        if (hasReadyGameLifecycleState("pause")) {
            when {
                GameGrubApp.isNeverSuspendMode() -> {
                    Timber.d("Game pause skipped due to suspend policy=never")
                }

                else -> {
                    GameGrubApp.xEnvironment?.onPause()
                    if (GameGrubApp.isManualSuspendMode()) {
                        GameGrubApp.isOverlayPaused = true
                        Timber.d("Game paused due to app backgrounded (manual resume required)")
                    } else {
                        Timber.d("Game paused due to app backgrounded")
                    }
                }
            }
        }
        PostHog.capture(event = "app_backgrounded")
        super.onPause()
    }

    override fun onStart() {
        super.onStart()
        orientationManager.startOrientator()
    }

    override fun onStop() {
        super.onStop()
        orientationManager.stopOrientator()
        SteamService.autoStopWhenIdle = true

        ServiceLifecycleManager.onStop(isChangingConfigurations)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var eventDispatched = GameGrubApp.events.emit(AndroidEvent.KeyEvent(event)) { keyEvent ->
            keyEvent.any { it }
        } == true

        if (!eventDispatched) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                if (SteamService.keepAlive) {
                    GameGrubApp.events.emit(AndroidEvent.BackPressed)
                    eventDispatched = true
                }
            }
        }

        return if (!eventDispatched) super.dispatchKeyEvent(event) else true
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        val eventDispatched = GameGrubApp.events.emit(AndroidEvent.MotionEvent(ev)) { event ->
            event.any { it }
        } == true

        return if (!eventDispatched) super.dispatchGenericMotionEvent(ev) else true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        immersiveModeManager.onWindowFocusChanged(hasFocus)
    }
}
