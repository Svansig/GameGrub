package app.gamegrub

import android.Manifest
import android.annotation.SuppressLint
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
import app.gamegrub.gateway.LaunchRequestGateway
import app.gamegrub.service.ServiceLifecycleManager
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.GameGrubMain
import app.gamegrub.ui.ImmersiveModeManager
import app.gamegrub.ui.OrientationManager
import app.gamegrub.ui.orientation.OrientationPolicy
import app.gamegrub.ui.runtime.XServerRuntime
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

    @Inject
    lateinit var launchRequestGateway: LaunchRequestGateway

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

        XServerRuntime.get().events.on<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        XServerRuntime.get().events.on<AndroidEvent.SetOrientationPolicy, Unit>(onSetOrientationPolicy)
        XServerRuntime.get().events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

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
        launchRequestGateway.handleLaunchIntent(this, intent, isNewIntent)
    }

    override fun onDestroy() {
        XServerRuntime.get().events.emit(AndroidEvent.ActivityDestroyed)

        XServerRuntime.get().events.off<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        XServerRuntime.get().events.off<AndroidEvent.SetOrientationPolicy, Unit>(onSetOrientationPolicy)
        XServerRuntime.get().events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)

        ServiceLifecycleManager.onDestroy(isChangingConfigurations)
        super.onDestroy()
    }

    private fun hasReadyGameLifecycleState(action: String): Boolean {
        if (!SteamService.keepAlive) return false
        if (!XServerRuntime.get().hasValidSuspendPolicyState()) {
            Timber.d("Skipping game %s because suspend policy state is not initialized", action)
            return false
        }
        if (XServerRuntime.get().xEnvironment == null) {
            Timber.d("Skipping game %s because xEnvironment is not ready", action)
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        XServerRuntime.get().setActivityInForeground(true)
        immersiveModeManager.applyImmersiveMode()

        SteamService.autoStopWhenIdle = false

        if (hasReadyGameLifecycleState("resume")) {
            when {
                XServerRuntime.get().isNeverSuspendMode() -> {
                    Timber.d("Game resume skipped due to suspend policy=never")
                }

                XServerRuntime.get().isOverlayPaused -> {
                    if (XServerRuntime.get().isManualSuspendMode()) {
                        Timber.d("Game remains suspended until user presses Resume")
                    }
                }

                else -> {
                    XServerRuntime.get().xEnvironment?.onResume()
                    Timber.d("Game resumed")
                }
            }
        }

        ServiceLifecycleManager.onResume(this)

        PostHog.capture(event = "app_foregrounded")
    }

    override fun onPause() {
        XServerRuntime.get().setActivityInForeground(false)
        if (hasReadyGameLifecycleState("pause")) {
            when {
                XServerRuntime.get().isNeverSuspendMode() -> {
                    Timber.d("Game pause skipped due to suspend policy=never")
                }

                else -> {
                    XServerRuntime.get().xEnvironment?.onPause()
                    if (XServerRuntime.get().isManualSuspendMode()) {
                        XServerRuntime.get().setOverlayPaused(true)
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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var eventDispatched = XServerRuntime.get().events.emit(AndroidEvent.KeyEvent(event)) { keyEvent ->
            keyEvent.any { it }
        } == true

        if (!eventDispatched) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                if (SteamService.keepAlive) {
                    XServerRuntime.get().events.emit(AndroidEvent.BackPressed)
                    eventDispatched = true
                }
            }
        }

        return if (!eventDispatched) super.dispatchKeyEvent(event) else true
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        val eventDispatched = XServerRuntime.get().events.emit(AndroidEvent.MotionEvent(ev)) { event ->
            event.any { it }
        } == true

        return if (!eventDispatched) super.dispatchGenericMotionEvent(ev) else true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        immersiveModeManager.onWindowFocusChanged(hasFocus)
    }
}
