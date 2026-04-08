package app.gamegrub.launch

import app.gamegrub.container.store.ContainerStore
import app.gamegrub.runtime.store.RuntimeStore
import app.gamegrub.session.model.EnvPlan
import app.gamegrub.session.model.MountPlan
import app.gamegrub.session.model.SessionComposition
import app.gamegrub.session.model.SessionMetadata
import app.gamegrub.session.model.SessionPlan
import app.gamegrub.session.model.SessionState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LaunchEngineActiveSessionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var launchEngine: LaunchEngine

    @Before
    fun setUp() {
        ActiveSessionStore.clearActiveSession()
        val rootDir = tempFolder.newFolder("store-root")
        launchEngine = LaunchEngine(RuntimeStore(rootDir), ContainerStore(rootDir))
    }

    @After
    fun tearDown() {
        ActiveSessionStore.clearActiveSession()
    }

    @Test
    fun `execute stores the session plan in ActiveSessionStore`() = runBlocking {
        val plan = makeSessionPlan("store-me-session")

        launchEngine.execute(plan, LaunchOptions(dryRun = true))

        val stored = ActiveSessionStore.getActiveSession()
        assertNotNull("ActiveSessionStore should hold the session plan after execute", stored)
        assertEquals("store-me-session", stored?.sessionId)
    }

    @Test
    fun `execute stores session with state LAUNCHING`() = runBlocking {
        val plan = makeSessionPlan("launching-state-session")

        launchEngine.execute(plan, LaunchOptions(dryRun = true))

        assertEquals(SessionState.LAUNCHING, ActiveSessionStore.getActiveSession()?.state)
    }

    @Test
    fun `execute preserves envPlan in stored session`() = runBlocking {
        val envPlan = EnvPlan(
            environmentVariables = mapOf(
                "DXVK_STATE_CACHE" to "/data/cache/dxvk",
                "MESA_SHADER_CACHE_DIR" to "/data/cache/mesa",
                "XDG_CACHE_HOME" to "/data/cache",
            ),
        )
        val plan = makeSessionPlan("env-session", envPlan)

        launchEngine.execute(plan, LaunchOptions(dryRun = true))

        val stored = ActiveSessionStore.getActiveSession()
        assertEquals("/data/cache/dxvk", stored?.envPlan?.environmentVariables?.get("DXVK_STATE_CACHE"))
        assertEquals("/data/cache/mesa", stored?.envPlan?.environmentVariables?.get("MESA_SHADER_CACHE_DIR"))
        assertEquals("/data/cache", stored?.envPlan?.environmentVariables?.get("XDG_CACHE_HOME"))
    }

    @Test
    fun `execute returns Success in dry run mode`() = runBlocking {
        val result = launchEngine.execute(makeSessionPlan("dry-run"), LaunchOptions(dryRun = true))

        assertTrue("Expected LaunchResult.Success in dry run", result is LaunchResult.Success)
        assertEquals("dry-run", (result as LaunchResult.Success).sessionId)
    }

    @Test
    fun `execute with failed composition stores session before returning Failure`() = runBlocking {
        // LaunchEngine stores the plan before inspecting composition, so even a Failed
        // composition should leave a session in the store during the execution window.
        val plan = makeSessionPlan("failed-composition")

        launchEngine.execute(plan, LaunchOptions(dryRun = false))

        // After execution with Failed composition, the session is in the store (EnvironmentSetupCoordinator
        // clears it after XEnvironment starts — that path is not reached in unit tests).
        val stored = ActiveSessionStore.getActiveSession()
        assertNotNull(stored)
        assertEquals("failed-composition", stored?.sessionId)
    }

    private fun makeSessionPlan(
        sessionId: String,
        envPlan: EnvPlan = EnvPlan(),
    ): SessionPlan = SessionPlan(
        sessionId = sessionId,
        metadata = SessionMetadata(
            sessionId = sessionId,
            gameId = "12345",
            gameTitle = "Test Game",
            gamePlatform = "STEAM",
        ),
        state = SessionState.ASSEMBLED,
        composition = SessionComposition.Failed(
            reason = "test-only — no real composition needed",
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
