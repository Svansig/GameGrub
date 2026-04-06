package app.gamegrub.storage

import app.gamegrub.enums.Marker
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for [StorageManager].
 */
class StorageManagerTest {
    /**
     * Verify marker lifecycle operations are handled by storage manager.
     */
    @Test
    fun markerLifecycle_addHasRemove_roundTripsSuccessfully() {
        val dir = createTempDir(prefix = "storage_marker_")
        try {
            assertFalse(StorageManager.hasMarker(dir.absolutePath, Marker.DOWNLOAD_COMPLETE_MARKER))
            assertTrue(StorageManager.addMarker(dir.absolutePath, Marker.DOWNLOAD_COMPLETE_MARKER))
            assertTrue(StorageManager.hasMarker(dir.absolutePath, Marker.DOWNLOAD_COMPLETE_MARKER))
            assertTrue(StorageManager.removeMarker(dir.absolutePath, Marker.DOWNLOAD_COMPLETE_MARKER))
            assertFalse(StorageManager.hasMarker(dir.absolutePath, Marker.DOWNLOAD_COMPLETE_MARKER))
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * Verify folder-size calculation returns non-zero for populated directories.
     */
    @Test
    fun getFolderSize_withFiles_returnsExpectedSize() = runBlocking {
        val dir = createTempDir(prefix = "storage_size_")
        try {
            val file = File(dir, "sample.bin")
            file.writeBytes(ByteArray(128))
            assertEquals(128L, StorageManager.getFolderSize(dir.absolutePath))
        } finally {
            dir.deleteRecursively()
        }
    }

    /**
     * Verify file read/write helpers round-trip content.
     */
    @Test
    fun readWriteString_roundTripsContent() {
        val dir = createTempDir(prefix = "storage_rw_")
        try {
            val file = File(dir, "nested/config.txt")
            StorageManager.writeStringToFile("hello", file.absolutePath)
            val value = StorageManager.readFileAsString(file.absolutePath)
            assertNotNull(value)
            assertTrue(value!!.contains("hello"))
        } finally {
            dir.deleteRecursively()
        }
    }
}
