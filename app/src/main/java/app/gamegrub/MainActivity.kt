package app.gamegrub

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.gamegrub.events.AndroidEvent
import app.gamegrub.service.ServiceLifecycleManager
import app.gamegrub.service.steam.SteamService
import app.gamegrub.ui.GameGrubMain
import app.gamegrub.ui.ImmersiveModeManager
import app.gamegrub.ui.OrientationManager
import app.gamegrub.ui.utils.AnimatedPngDecoder
import app.gamegrub.ui.utils.IconDecoder
import app.gamegrub.ui.utils.LocaleHelper
import app.gamegrub.utils.container.ContainerUtils
import coil.ImageLoader
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.posthog.PostHog
import com.skydoves.landscapist.coil.LocalCoilImageLoader
import com.winlator.core.AppUtils
import com.winlator.inputcontrols.ControllerManager
import dagger.hilt.android.AndroidEntryPoint
import okio.Path.Companion.toOkioPath
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var immersiveModeManager: ImmersiveModeManager
    private lateinit var orientationManager: OrientationManager

    private val onSetSystemUi: (AndroidEvent.SetSystemUIVisibility) -> Unit = {
        immersiveModeManager.setSystemUIVisibility(it.visible)
    }

    private val onSetAllowedOrientation: (AndroidEvent.SetAllowedOrientation) -> Unit = {
        orientationManager.setAllowedOrientations(it.orientations)
    }

    private val onStartOrientator: (AndroidEvent.StartOrientator) -> Unit = {
        orientationManager.startOrientator()
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

        ControllerManager.getInstance().init(applicationContext)
        ContainerUtils.setContainerDefaults(applicationContext)

        LaunchRequestManager.handleLaunchIntent(this, intent)

        AppUtils.keepScreenOn(this)

        GameGrubApp.events.on<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        GameGrubApp.events.on<AndroidEvent.StartOrientator, Unit>(onStartOrientator)
        GameGrubApp.events.on<AndroidEvent.SetAllowedOrientation, Unit>(onSetAllowedOrientation)
        GameGrubApp.events.on<AndroidEvent.EndProcess, Unit>(onEndProcess)

        setContent {
            var hasNotificationPermission by remember { mutableStateOf(false) }
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            LaunchedEffect(Unit) {
                if (!hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val context = LocalContext.current
            val imageLoader = remember {
                val memoryCache = MemoryCache.Builder(context)
                    .maxSizePercent(0.1)
                    .strongReferencesEnabled(true)
                    .build()

                val diskCache = DiskCache.Builder()
                    .maxSizePercent(0.03)
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .build()

                ImageLoader.Builder(context)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCache(memoryCache)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCache(diskCache)
                    .components {
                        add(
                            Interceptor { chain ->
                                val request = if (!NetworkMonitor.hasInternet.value) {
                                    chain.request.newBuilder()
                                        .networkCachePolicy(CachePolicy.DISABLED)
                                        .build()
                                } else {
                                    chain.request
                                }
                                chain.proceed(request)
                            },
                        )
                        add(IconDecoder.Factory())
                        add(AnimatedPngDecoder.Factory())
                    }
                    .build()
            }

            CompositionLocalProvider(LocalCoilImageLoader provides imageLoader) {
                GameGrubMain()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LaunchRequestManager.handleLaunchIntent(this, intent, isNewIntent = true)
    }

    override fun onDestroy() {
        super.onDestroy()

        GameGrubApp.events.emit(AndroidEvent.ActivityDestroyed)

        GameGrubApp.events.off<AndroidEvent.SetSystemUIVisibility, Unit>(onSetSystemUi)
        GameGrubApp.events.off<AndroidEvent.StartOrientator, Unit>(onStartOrientator)
        GameGrubApp.events.off<AndroidEvent.SetAllowedOrientation, Unit>(onSetAllowedOrientation)
        GameGrubApp.events.off<AndroidEvent.EndProcess, Unit>(onEndProcess)

        ServiceLifecycleManager.onDestroy(isChangingConfigurations)
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        immersiveModeManager.onWindowFocusChanged(hasFocus)
    }
}
