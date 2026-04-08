package app.gamegrub.container.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerManifestTest {
    @Test
    fun containerManifest_validatesCorrectly() {
        val valid = ContainerManifest(
            id = "steam_12345",
            name = "Test Game",
            gameId = "12345",
            gamePlatform = "STEAM",
            baseId = "base-linux-glibc2.35-2.35",
            runtimeId = "wine-8.0-glibc2.35",
            driverId = "turnip-merged-2024-03-01",
            profileId = "wine-esync-modern",
        )
        assertTrue(valid.isValid())
    }

    @Test
    fun containerManifest_failsWithBlankFields() {
        val invalid = ContainerManifest(
            id = "",
            name = "",
            gameId = "",
            gamePlatform = "",
            baseId = "",
            runtimeId = "",
        )
        val errors = invalid.validate()
        assertEquals(6, errors.size)
    }

    @Test
    fun containerManifest_serializationRoundtrip() {
        val original = ContainerManifest(
            id = "gog_123",
            name = "GOG Game",
            createdAt = 1234567890000L,
            lastModified = 1234567900000L,
            gameId = "123",
            gamePlatform = "GOG",
            baseId = "base-linux-glibc2.35-2.35",
            runtimeId = "proton-9.0-arm64ec",
            driverId = "mesa-24.0",
            profileId = "proton-modern",
            configuration = ContainerConfiguration(
                screenSize = "1920x1080",
                renderer = "vulkan",
                csmt = true,
                containerVariant = "glibc",
            ),
            userOverrides = mapOf("WINEDLLOVERRIDES" to "msvcp140=n;b"),
        )
        val json = kotlinx.serialization.json.Json.encodeToString(original)
        val restored = kotlinx.serialization.json.Json.decodeFromString<ContainerManifest>(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.gamePlatform, restored.gamePlatform)
        assertEquals(original.configuration.screenSize, restored.configuration.screenSize)
    }
}