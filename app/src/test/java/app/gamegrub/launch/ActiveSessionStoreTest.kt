package app.gamegrub.launch

import app.gamegrub.session.model.EnvPlan
import app.gamegrub.session.model.MountPlan
import app.gamegrub.session.model.SessionComposition
import app.gamegrub.session.model.SessionMetadata
import app.gamegrub.session.model.SessionPlan
import app.gamegrub.session.model.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ActiveSessionStoreTest {

    @Before
    fun setUp() {
        ActiveSessionStore.clearActiveSession()
    }

    @After
    fun tearDown() {
        ActiveSessionStore.clearActiveSession()
    }

    @Test
    fun `initially no active session`() {
        assertNull(ActiveSessionStore.getActiveSession())
    }

    @Test
    fun `setActiveSession stores the session`() {
        val plan = makeSessionPlan("session-abc")

        ActiveSessionStore.setActiveSession(plan)

        assertEquals(plan, ActiveSessionStore.getActiveSession())
    }

    @Test
    fun `clearActiveSession removes the stored session`() {
        ActiveSessionStore.setActiveSession(makeSessionPlan("session-abc"))

        ActiveSessionStore.clearActiveSession()

        assertNull(ActiveSessionStore.getActiveSession())
    }

    @Test
    fun `setActiveSession replaces a previous session`() {
        ActiveSessionStore.setActiveSession(makeSessionPlan("session-first"))
        ActiveSessionStore.setActiveSession(makeSessionPlan("session-second"))

        assertEquals("session-second", ActiveSessionStore.getActiveSession()?.sessionId)
    }

    @Test
    fun `clearActiveSession is safe when no session is set`() {
        assertNull(ActiveSessionStore.getActiveSession())
        ActiveSessionStore.clearActiveSession()
        assertNull(ActiveSessionStore.getActiveSession())
    }

    @Test
    fun `getActiveSession reflects env plan of stored session`() {
        val envPlan = EnvPlan(
            environmentVariables = mapOf(
                "DXVK_STATE_CACHE" to "/cache/dxvk",
                "XDG_CACHE_HOME" to "/cache",
            ),
        )
        ActiveSessionStore.setActiveSession(makeSessionPlan("session-env", envPlan))

        val stored = ActiveSessionStore.getActiveSession()
        assertEquals("/cache/dxvk", stored?.envPlan?.environmentVariables?.get("DXVK_STATE_CACHE"))
        assertEquals("/cache", stored?.envPlan?.environmentVariables?.get("XDG_CACHE_HOME"))
    }

    private fun makeSessionPlan(
        sessionId: String,
        envPlan: EnvPlan = EnvPlan(),
    ): SessionPlan = SessionPlan(
        sessionId = sessionId,
        metadata = SessionMetadata(
            sessionId = sessionId,
            gameId = "99999",
            gameTitle = "Test Game",
            gamePlatform = "STEAM",
        ),
        state = SessionState.ASSEMBLED,
        composition = SessionComposition.Failed(
            reason = "test-only composition",
            missingComponents = emptyList(),
        ),
        mountPlan = MountPlan(
            baseMount = "",
            runtimeMount = "",
            driverMount = null,
            containerPrefixMount = "",
            containerInstallMount = "",
            containerSavesMount = null,
            containerCacheMount = "",
            tempDirMount = "",
        ),
        envPlan = envPlan,
        cacheHandles = emptyList(),
    )
}
