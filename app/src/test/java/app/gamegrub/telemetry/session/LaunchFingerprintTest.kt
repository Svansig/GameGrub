package app.gamegrub.telemetry.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchFingerprintTest {
    @Test
    fun fingerprint_hasDefaultSessionId() {
        val fingerprint = LaunchFingerprint()
        assertNotNull(fingerprint.sessionId)
        assertTrue(fingerprint.sessionId.isNotBlank())
    }

    @Test
    fun fingerprint_hasDefaultTimestamp() {
        val before = System.currentTimeMillis()
        val fingerprint = LaunchFingerprint()
        val after = System.currentTimeMillis()
        assertTrue(fingerprint.timestamp in before..after)
    }

    @Test
    fun fingerprint_serialization_roundtrip() {
        val original = LaunchFingerprint(
            sessionId = "test-session-123",
            runtimeId = "wine-8.0",
            driverId = "turnip-2024",
            containerId = "steam_12345",
            gameTitle = "Test Game",
            gamePlatform = "STEAM",
            wineVersion = "proton-9.0-arm64ec",
            dxwrapper = "dxvk",
            containerVariant = "glibc",
        )
        val json = original.toJson()
        val restored = LaunchFingerprint.fromJson(json)
        assertEquals(original.sessionId, restored.sessionId)
        assertEquals(original.runtimeId, restored.runtimeId)
        assertEquals(original.driverId, restored.driverId)
        assertEquals(original.containerId, restored.containerId)
        assertEquals(original.gameTitle, restored.gameTitle)
        assertEquals(original.gamePlatform, restored.gamePlatform)
        assertEquals(original.wineVersion, restored.wineVersion)
        assertEquals(original.dxwrapper, restored.dxwrapper)
        assertEquals(original.containerVariant, restored.containerVariant)
    }

    @Test
    fun fingerprint_emitter_storesRecent() {
        LaunchFingerprintEmitter.clear()
        val fp1 = LaunchFingerprint(sessionId = "session-1")
        val fp2 = LaunchFingerprint(sessionId = "session-2")
        LaunchFingerprintEmitter.emit(fp1)
        LaunchFingerprintEmitter.emit(fp2)
        val recent = LaunchFingerprintEmitter.getRecent(2)
        assertEquals(2, recent.size)
        assertEquals("session-2", recent[0].sessionId)
        assertEquals("session-1", recent[1].sessionId)
    }

    @Test
    fun fingerprint_emitter_findBySessionId() {
        LaunchFingerprintEmitter.clear()
        val fp = LaunchFingerprint(sessionId = "find-me")
        LaunchFingerprintEmitter.emit(fp)
        val found = LaunchFingerprintEmitter.getBySessionId("find-me")
        assertEquals("find-me", found?.sessionId)
    }
}