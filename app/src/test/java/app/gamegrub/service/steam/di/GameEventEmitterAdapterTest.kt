package app.gamegrub.service.steam.di

import app.gamegrub.GameGrubApp
import app.gamegrub.events.AndroidEvent
import app.gamegrub.events.SteamEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameEventEmitterAdapterTest {
    private val adapter = GameEventEmitterAdapter()

    @Before
    fun setUp() {
        GameGrubApp.events.clearAllListeners()
    }

    @After
    fun tearDown() {
        GameGrubApp.events.clearAllListeners()
    }

    @Test
    fun emitSteamEvent_dispatchesToEventBus() {
        var receivedChallengeUrl: String? = null

        GameGrubApp.events.on<SteamEvent.QrChallengeReceived, Unit> { event ->
            receivedChallengeUrl = event.challengeUrl
        }

        adapter.emitSteamEvent(SteamEvent.QrChallengeReceived("https://steam.test/challenge"))

        assertEquals("https://steam.test/challenge", receivedChallengeUrl)
    }

    @Test
    fun emitAndroidEvent_dispatchesToEventBus() {
        var backPressedReceived = false

        GameGrubApp.events.on<AndroidEvent.BackPressed, Unit> {
            backPressedReceived = true
        }

        adapter.emitAndroidEvent(AndroidEvent.BackPressed)

        assertEquals(true, backPressedReceived)
    }
}

