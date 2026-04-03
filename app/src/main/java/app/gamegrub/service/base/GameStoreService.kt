package app.gamegrub.service.base

import android.app.Service
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class GameStoreService : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    protected var backgroundSyncJob: Job? = null
    protected var syncInProgress = false
    protected var lastSyncTimestamp = 0L
    protected var hasPerformedInitialSync = false

    companion object {
        const val ACTION_SYNC_LIBRARY = "app.gamegrub.SYNC_LIBRARY"
        const val ACTION_MANUAL_SYNC = "app.gamegrub.MANUAL_SYNC"
        const val SYNC_THROTTLE_MILLIS = 15 * 60 * 1000L
    }

    protected abstract fun getServiceTag(): String

    protected abstract fun performSync(context: Context, isManual: Boolean)

    protected abstract fun getNotificationTitle(): String

    protected abstract fun getNotificationContent(): String

    protected fun startServiceWithSync(context: Context) {
        if (!hasPerformedInitialSync) {
            Timber.tag(getServiceTag()).i("First-time start - starting service with initial sync")
            val intent = Intent(context, this::class.java)
            intent.action = ACTION_SYNC_LIBRARY
            context.startForegroundService(intent)
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLastSync = now - lastSyncTimestamp

        val intent = Intent(context, this::class.java)
        if (timeSinceLastSync >= SYNC_THROTTLE_MILLIS) {
            Timber.tag(getServiceTag()).i("Starting service with automatic sync (throttle passed)")
            intent.action = ACTION_SYNC_LIBRARY
        } else {
            val remainingMinutes = (SYNC_THROTTLE_MILLIS - timeSinceLastSync) / 1000 / 60
            Timber.tag(getServiceTag()).d("Starting service without sync - throttled (${remainingMinutes}min remaining)")
        }
        context.startForegroundService(intent)
    }

    protected fun triggerManualSync(context: Context) {
        Timber.tag(getServiceTag()).i("Triggering manual library sync (bypasses throttle)")
        val intent = Intent(context, this::class.java)
        intent.action = ACTION_MANUAL_SYNC
        startForegroundService(intent)
    }

    protected fun handleStartCommand(intent: Intent?) {
        when (intent?.action) {
            ACTION_SYNC_LIBRARY -> runSync(isManual = false)
            ACTION_MANUAL_SYNC -> runSync(isManual = true)
        }
    }

    private fun runSync(isManual: Boolean) {
        if (syncInProgress) {
            Timber.tag(getServiceTag()).w("Sync already in progress, skipping")
            return
        }

        syncInProgress = true
        backgroundSyncJob = serviceScope.launch {
            try {
                performSync(this@GameStoreService, isManual)
                lastSyncTimestamp = System.currentTimeMillis()
                hasPerformedInitialSync = true
            } catch (e: Exception) {
                Timber.tag(getServiceTag()).e(e, "Sync failed")
            } finally {
                syncInProgress = false
            }
        }
    }

    protected fun stopSelfWithDelay() {
        serviceScope.launch {
            delay(2000)
            stopSelf()
        }
    }

    override fun onDestroy() {
        backgroundSyncJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
