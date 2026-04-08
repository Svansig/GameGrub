package app.gamegrub.launch

import app.gamegrub.session.model.SessionPlan
import timber.log.Timber

/**
 * Process-lifetime store for the session plan assembled prior to XEnvironment launch.
 *
 * Bridges the gap between session assembly (which runs before onSuccess in
 * GameLaunchOrchestrator) and the XEnvironment setup (which runs inside XServerScreen).
 * Allows EnvironmentSetupCoordinator to consume EnvPlan vars without threading
 * SessionPlan through UI layers.
 *
 * Lifecycle: set by LaunchEngine.execute(), consumed and cleared by
 * EnvironmentSetupCoordinator.setupXEnvironment().
 */
object ActiveSessionStore {
    @Volatile
    private var activeSession: SessionPlan? = null

    fun setActiveSession(plan: SessionPlan) {
        activeSession = plan
        Timber.d("ActiveSessionStore: session set ${plan.sessionId}")
    }

    fun getActiveSession(): SessionPlan? = activeSession

    fun clearActiveSession() {
        val id = activeSession?.sessionId
        activeSession = null
        if (id != null) Timber.d("ActiveSessionStore: session cleared (was $id)")
    }
}
