package app.gamegrub.telemetry.record

import app.gamegrub.session.model.LaunchOutcome
import app.gamegrub.session.model.SessionMilestone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LaunchRecordStore @Inject constructor(
    private val rootDir: File,
) {
    private val json = Json { prettyPrint = true }
    private val recordsDir: File by lazy { File(rootDir, "telemetry/records").also { it.mkdirs() } }

    suspend fun saveRecord(record: LaunchSessionRecord): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(recordsDir, "${record.sessionId}.json")
            file.writeText(json.encodeToString(record))
            Timber.d("Saved launch record: ${record.sessionId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save launch record: ${record.sessionId}")
            Result.failure(e)
        }
    }

    suspend fun getRecord(sessionId: String): LaunchSessionRecord? = withContext(Dispatchers.IO) {
        try {
            val file = File(recordsDir, "$sessionId.json")
            if (file.exists()) {
                json.decodeFromString<LaunchSessionRecord>(file.readText())
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to read launch record: $sessionId")
            null
        }
    }

    suspend fun getRecordsByTitle(titleId: String): List<LaunchSessionRecord> = withContext(Dispatchers.IO) {
        recordsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<LaunchSessionRecord>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?.filter { it.titleId == titleId }
            ?: emptyList()
    }

    suspend fun getRecentRecords(limit: Int = 50): List<LaunchSessionRecord> = withContext(Dispatchers.IO) {
        recordsDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.take(limit)
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<LaunchSessionRecord>(file.readText())
                } catch (e: Exception) {
                    null
                }
            }
            ?: emptyList()
    }

    suspend fun getSuccessfulRecords(): List<LaunchSessionRecord> = withContext(Dispatchers.IO) {
        getRecentRecords().filter { it.outcome == LaunchOutcome.SUCCESS }
    }

    suspend fun getFailedRecords(): List<LaunchSessionRecord> = withContext(Dispatchers.IO) {
        getRecentRecords().filter { it.outcome == LaunchOutcome.FAILURE }
    }

    suspend fun getLastKnownGood(titleId: String): LaunchSessionRecord? = withContext(Dispatchers.IO) {
        getRecordsByTitle(titleId)
            .filter { it.outcome == LaunchOutcome.SUCCESS }
            .maxByOrNull { it.endTime }
    }

    suspend fun deleteRecord(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(recordsDir, "$sessionId.json")
            if (file.exists()) {
                file.delete()
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete launch record: $sessionId")
            false
        }
    }

    suspend fun getRecordCount(): Int = withContext(Dispatchers.IO) {
        recordsDir.listFiles()?.count { it.extension == "json" } ?: 0
    }

    suspend fun clearOldRecords(maxAgeMs: Long): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - maxAgeMs
        val oldFiles = recordsDir.listFiles()
            ?.filter { it.extension == "json" && it.lastModified() < cutoff }
            ?: emptyList()

        var deleted = 0
        oldFiles.forEach { file ->
            if (file.delete()) deleted++
        }

        if (deleted > 0) {
            Timber.i("Cleared $deleted old launch records")
        }
        deleted
    }
}