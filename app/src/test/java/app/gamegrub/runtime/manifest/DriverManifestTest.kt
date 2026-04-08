package app.gamegrub.runtime.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverManifestTest {
    @Test
    fun driverManifest_validatesCorrectly() {
        val valid = DriverManifest(
            id = "turnip-merged-2024-03-01",
            version = "2024.03.01",
            contentHash = "d".repeat(64),
            driverPath = "/drivers/turnip-merged-2024-03-01",
            driverType = DriverType.TURNIP,
            minGlibcVersion = "2.35",
        )
        assertTrue(valid.isValid())
        assertEquals(DriverType.TURNIP, valid.driverType)
    }

    @Test
    fun driverManifest_failsWithInvalidHash() {
        val invalid = DriverManifest(
            id = "turnip-merged-2024-03-01",
            version = "2024.03.01",
            contentHash = "invalid",
            driverPath = "/drivers/turnip-merged-2024-03-01",
            driverType = DriverType.VULKAN,
        )
        assertFalse(invalid.isValid())
    }

    @Test
    fun driverManifest_serializationRoundtrip() {
        val original = DriverManifest(
            id = "mesa-24.0",
            version = "24.0.0",
            contentHash = "e".repeat(64),
            driverPath = "/drivers/mesa-24.0",
            driverType = DriverType.VULKAN,
            minGlibcVersion = "2.31",
        )
        val json = kotlinx.serialization.json.Json.encodeToString(original)
        val restored = kotlinx.serialization.json.Json.decodeFromString<DriverManifest>(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.driverType, restored.driverType)
    }
}