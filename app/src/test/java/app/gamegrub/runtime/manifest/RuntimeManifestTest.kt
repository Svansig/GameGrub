package app.gamegrub.runtime.manifest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeManifestTest {
    @Test
    fun baseManifest_validatesCorrectly() {
        val valid = BaseManifest(
            id = "base-linux-glibc2.35-2.35",
            version = "2.35.0",
            contentHash = "a".repeat(64),
            rootfsPath = "/bases/base-linux-glibc2.35-2.35",
            description = "Glibc 2.35 base",
        )
        assertTrue(valid.isValid())
        assertTrue(valid.validate().isEmpty())
    }

    @Test
    fun baseManifest_failsWithInvalidHash() {
        val invalid = BaseManifest(
            id = "base-linux-glibc2.35-2.35",
            version = "2.35.0",
            contentHash = "short",
            rootfsPath = "/bases/base-linux-glibc2.35-2.35",
        )
        assertFalse(invalid.isValid())
        assertTrue(invalid.validate().any { it.contains("contentHash") })
    }

    @Test
    fun baseManifest_failsWithBlankFields() {
        val invalid = BaseManifest(
            id = "",
            version = "",
            contentHash = "",
            rootfsPath = "",
        )
        val errors = invalid.validate()
        assertEquals(4, errors.size)
    }

    @Test
    fun baseManifest_serializationRoundtrip() {
        val original = BaseManifest(
            id = "base-linux-glibc2.35-2.35",
            version = "2.35.0",
            contentHash = "abc123def456789012345678901234567890123456789012345678901234",
            rootfsPath = "/bases/base-linux-glibc2.35-2.35",
            description = "Glibc 2.35 base for ARM64",
            createdAt = 1234567890000L,
        )
        val json = kotlinx.serialization.json.Json.encodeToString(original)
        val restored = kotlinx.serialization.json.Json.decodeFromString<BaseManifest>(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.version, restored.version)
        assertEquals(original.contentHash, restored.contentHash)
        assertEquals(original.rootfsPath, restored.rootfsPath)
        assertEquals(original.description, restored.description)
        assertEquals(original.createdAt, restored.createdAt)
    }

    @Test
    fun runtimeManifest_validatesCorrectly() {
        val valid = RuntimeManifest(
            id = "wine-8.0-glibc2.35",
            version = "8.0",
            contentHash = "b".repeat(64),
            runtimePath = "/runtimes/wine-8.0",
            baseId = "base-linux-glibc2.35-2.35",
            runtimeType = RuntimeType.WINE,
            metadata = RuntimeMetadata(
                dxvkVersion = "1.10",
                vkd3dVersion = "1.7",
            ),
        )
        assertTrue(valid.isValid())
        assertEquals(RuntimeType.WINE, valid.runtimeType)
    }

    @Test
    fun runtimeManifest_failsWithMissingBaseId() {
        val invalid = RuntimeManifest(
            id = "wine-8.0-glibc2.35",
            version = "8.0",
            contentHash = "b".repeat(64),
            runtimePath = "/runtimes/wine-8.0",
            baseId = "",
            runtimeType = RuntimeType.PROTON,
        )
        assertFalse(invalid.isValid())
        assertTrue(invalid.validate().any { it.contains("baseId") })
    }

    @Test
    fun runtimeManifest_serializationRoundtrip() {
        val original = RuntimeManifest(
            id = "proton-9.0-arm64ec",
            version = "9.0-1",
            contentHash = "c".repeat(64),
            runtimePath = "/runtimes/proton-9.0-arm64ec",
            baseId = "base-linux-glibc2.35-2.35",
            runtimeType = RuntimeType.PROTON,
            metadata = RuntimeMetadata(
                protonVersion = "9.0-1",
                dxvkVersion = "1.11",
                vkd3dVersion = "1.9",
                wineBuild = "Wine 9.0",
            ),
        )
        val json = kotlinx.serialization.json.Json.encodeToString(original)
        val restored = kotlinx.serialization.json.Json.decodeFromString<RuntimeManifest>(json)
        assertEquals(original.id, restored.id)
        assertEquals(original.runtimeType, restored.runtimeType)
        assertEquals(original.metadata.dxvkVersion, restored.metadata.dxvkVersion)
    }

    @Test
    fun sha256Validation() {
        assertTrue(BaseManifest.isValidSha256("abc123def456789012345678901234567890123456789012345678901234"))
        assertFalse(BaseManifest.isValidSha256("too_short"))
        assertFalse(BaseManifest.isValidSha256("zzz123def45678901234567890123456789012345678901234567890123g"))
    }
}