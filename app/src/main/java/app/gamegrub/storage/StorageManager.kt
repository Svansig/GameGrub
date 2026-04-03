package app.gamegrub.storage

import android.content.res.AssetManager
import android.os.StatFs
import app.gamegrub.enums.Marker
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import timber.log.Timber

/**
 * Central manager for storage-related operations.
 *
 * This object owns marker lifecycle, filesystem reads/writes, path traversal, and storage
 * capacity helpers. Callers outside the storage package should delegate all storage work here.
 */
object StorageManager {
    /**
     * Check if a marker file exists in a directory.
     * @param dirPath Directory path to inspect.
     * @param type Marker type.
     * @return `true` when the marker file exists.
     */
    fun hasMarker(dirPath: String, type: Marker): Boolean = File(dirPath, type.fileName).exists()

    /**
     * Create a marker file in the target directory.
     * @param dirPath Directory where marker should be written.
     * @param type Marker type.
     * @return `true` when marker exists after this call.
     */
    fun addMarker(dirPath: String, type: Marker): Boolean {
        val dir = File(dirPath)
        if (File(dir, type.fileName).exists()) {
            Timber.i("Marker %s at %s already exists", type.fileName, dirPath)
            return true
        }
        if (dir.exists()) {
            return try {
                File(dir, type.fileName).createNewFile()
                Timber.i("Added marker %s at %s", type.fileName, dirPath)
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to add marker %s at %s", type.fileName, dirPath)
                false
            }
        }
        Timber.e("Marker %s at %s not added as directory not found", type.fileName, dirPath)
        return false
    }

    /**
     * Remove a marker file if it exists.
     * @param dirPath Directory where marker should be removed.
     * @param type Marker type.
     * @return `true` when marker does not exist after this call.
     */
    fun removeMarker(dirPath: String, type: Marker): Boolean {
        val marker = File(dirPath, type.fileName)
        return if (marker.exists()) marker.delete() else true
    }

    /**
     * Resolve available bytes for a filesystem path.
     * @param path Filesystem path.
     * @return Available bytes.
     */
    fun getAvailableSpace(path: String): Long {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Invalid path: $path")
        }
        val stat = StatFs(path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    /**
     * Compute folder size recursively.
     * @param folderPath Folder path.
     * @return Total bytes in folder.
     */
    suspend fun getFolderSize(folderPath: String): Long {
        val folder = File(folderPath)
        if (!folder.exists()) return 0L
        var bytes = 0L
        folder.walk().forEach {
            bytes += it.length()
            yield()
        }
        return bytes
    }

    /**
     * Format bytes in binary-size notation.
     * @param bytes Raw bytes.
     * @param decimalPlaces Fraction digits for non-byte units.
     * @return Formatted size string.
     */
    fun formatBinarySize(bytes: Long, decimalPlaces: Int = 2): String {
        require(bytes > Long.MIN_VALUE) { "Out of range" }
        require(decimalPlaces >= 0) { "Negative decimal places unsupported" }

        val isNegative = bytes < 0
        val absBytes = kotlin.math.abs(bytes)

        if (absBytes < 1024) return "$bytes B"

        val units = arrayOf("KiB", "MiB", "GiB", "TiB", "PiB")
        val digitGroups = (63 - absBytes.countLeadingZeroBits()) / 10
        val value = absBytes.toDouble() / (1L shl (digitGroups * 10))

        val signedValue = if (isNegative) -value else value
        return "%.${decimalPlaces}f %s".trim().format(signedValue, units[digitGroups - 1])
    }

    /**
     * Move content between two directory trees with progress callbacks.
     * @param sourceDir Source directory.
     * @param targetDir Target directory.
     * @param onProgressUpdate Progress callback for each file.
     * @param onComplete Completion callback.
     */
    suspend fun moveGamesFromOldPath(
        sourceDir: String,
        targetDir: String,
        onProgressUpdate: (currentFile: String, fileProgress: Float, movedFiles: Int, totalFiles: Int) -> Unit,
        onComplete: () -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val sourcePath = Paths.get(sourceDir)
            val targetPath = Paths.get(targetDir)

            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath)
            }

            val allFiles = mutableListOf<Path>()
            Files.walkFileTree(
                sourcePath,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (Files.isRegularFile(file)) {
                            allFiles.add(file)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                        Timber.e(exc, "Failed to visit file: %s", file)
                        return FileVisitResult.CONTINUE
                    }
                },
            )

            val totalFiles = allFiles.size
            var filesMoved = 0

            for (sourceFilePath in allFiles) {
                val relativePath = sourceFilePath.subpath(Paths.get(sourceDir).nameCount, sourceFilePath.nameCount)
                val targetFilePath = Paths.get(targetDir, relativePath.toString())
                Files.createDirectories(targetFilePath.parent)

                try {
                    Files.move(sourceFilePath, targetFilePath, StandardCopyOption.ATOMIC_MOVE)
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(relativePath.toString(), 1f, filesMoved++, totalFiles)
                    }
                } catch (e: Exception) {
                    val fileSize = Files.size(sourceFilePath)
                    var bytesCopied = 0L

                    FileChannel.open(sourceFilePath, StandardOpenOption.READ).use { sourceChannel ->
                        FileChannel.open(
                            targetFilePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                        ).use { targetChannel ->
                            val buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
                            var bytesRead: Int
                            while (sourceChannel.read(buffer).also { bytesRead = it } > 0) {
                                buffer.flip()
                                targetChannel.write(buffer)
                                buffer.compact()
                                bytesCopied += bytesRead

                                val fileProgress = if (fileSize > 0) bytesCopied.toFloat() / fileSize else 1f
                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(relativePath.toString(), fileProgress, filesMoved, totalFiles)
                                }
                            }
                            targetChannel.force(true)
                        }
                    }

                    Files.delete(sourceFilePath)
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(relativePath.toString(), 1f, filesMoved++, totalFiles)
                    }
                }
            }

            Files.walkFileTree(
                sourcePath,
                object : SimpleFileVisitor<Path>() {
                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc == null) {
                            try {
                                var isEmpty = true
                                Files.newDirectoryStream(dir).use { stream ->
                                    if (stream.iterator().hasNext()) {
                                        isEmpty = false
                                    }
                                }
                                if (isEmpty && dir != sourcePath) {
                                    Files.delete(dir)
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to delete directory: %s", dir)
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }
                },
            )

            try {
                var isEmpty = true
                Files.newDirectoryStream(sourcePath).use { stream ->
                    if (stream.iterator().hasNext()) {
                        isEmpty = false
                    }
                }
                if (isEmpty) {
                    Files.delete(sourcePath)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            withContext(Dispatchers.Main) { onComplete() }
        } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    /**
     * Calculate total size of a directory recursively.
     * @param directory Directory to inspect.
     * @return Total size in bytes.
     */
    fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            if (!directory.exists() || !directory.isDirectory) return 0L
            val files = directory.listFiles() ?: return 0L
            for (file in files) {
                size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error calculating directory size for %s", directory.name)
        }
        return size
    }

    /**
     * Create a directory tree.
     * @param dirName Target directory path.
     */
    fun makeDir(dirName: String) {
        File(dirName).mkdirs()
    }

    /**
     * Ensure a file exists.
     * @param fileName File path.
     * @param errorTag Log tag.
     * @param errorMsg Optional error formatter.
     */
    fun makeFile(fileName: String, errorTag: String? = "StorageManager", errorMsg: ((Exception) -> String)? = null) {
        try {
            val file = File(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in makeFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error creating file: $e")
        }
    }

    /**
     * Create parent path for a file/directory if missing.
     * @param filepath Target path.
     */
    fun createPathIfNotExist(filepath: String) {
        val file = File(filepath)
        var dirs = filepath
        if (!filepath.endsWith('/') && filepath.lastIndexOf('/') > 0) {
            dirs = file.parent!!
        }
        makeDir(dirs)
    }

    /**
     * Read a file into a string.
     * @param path File path.
     * @param errorTag Log tag.
     * @param errorMsg Optional error formatter.
     * @return File content or `null` on error.
     */
    fun readFileAsString(path: String, errorTag: String = "StorageManager", errorMsg: ((Exception) -> String)? = null): String? {
        return try {
            BufferedReader(FileReader(path)).use { reader ->
                val total = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    total.append(line).append('\n')
                }
                total.toString()
            }
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in readFileAsString()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error reading file: $e")
            null
        }
    }

    /**
     * Write string data to a file path.
     * @param data Data to write.
     * @param path File path.
     * @param errorTag Log tag.
     * @param errorMsg Optional error formatter.
     */
    fun writeStringToFile(data: String, path: String, errorTag: String? = "StorageManager", errorMsg: ((Exception) -> String)? = null) {
        createPathIfNotExist(path)
        try {
            FileOutputStream(path).use { output ->
                OutputStreamWriter(output).use { writer ->
                    writer.append(data)
                }
                output.flush()
            }
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in writeStringToFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error writing to file: $e")
        }
    }

    /**
     * Walk a directory and apply an action to each entry.
     * @param rootPath Root path.
     * @param maxDepth Maximum recursion depth, -1 for unlimited.
     * @param action Callback for each discovered path.
     */
    fun walkThroughPath(rootPath: Path, maxDepth: Int = 0, action: (Path) -> Unit) {
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) return
        Files.list(rootPath).use { fileList ->
            fileList.forEach {
                action(it)
                if (maxDepth != 0 && it.exists() && it.isDirectory()) {
                    walkThroughPath(it, if (maxDepth > 0) maxDepth - 1 else maxDepth, action)
                }
            }
        }
    }

    /**
     * Find files in a directory using wildcard parts.
     * @param rootPath Root path.
     * @param pattern Pattern containing `*` segments.
     * @param includeDirectories Whether directories can match.
     * @return Stream of matching paths.
     */
    fun findFiles(rootPath: Path, pattern: String, includeDirectories: Boolean = false): Stream<Path> {
        val patternParts = pattern.split("*").filter { it.isNotEmpty() }
        Timber.i("%s -> %s", pattern, patternParts)
        if (!Files.exists(rootPath)) return emptyList<Path>().stream()
        return Files.list(rootPath).filter { path ->
            if (path.isDirectory() && !includeDirectories) {
                false
            } else {
                val fileName = path.name
                var startIndex = 0
                !patternParts.map {
                    val index = fileName.indexOf(it, startIndex)
                    if (index >= 0) {
                        startIndex = index + it.length
                    }
                    index
                }.any { it < 0 }
            }
        }
    }

    /**
     * Find files recursively using wildcard parts.
     * @param rootPath Root path.
     * @param pattern Pattern containing `*` segments.
     * @param maxDepth Maximum recursion depth.
     * @param includeDirectories Whether directories can match.
     * @return Stream of matching paths.
     */
    fun findFilesRecursive(
        rootPath: Path,
        pattern: String,
        maxDepth: Int = -1,
        includeDirectories: Boolean = false,
    ): Stream<Path> {
        val patternParts = pattern.split("*").filter { it.isNotEmpty() }
        if (!Files.exists(rootPath)) return emptyList<Path>().stream()

        val results = mutableListOf<Path>()

        fun matches(fileName: String): Boolean {
            var startIndex = 0
            for (part in patternParts) {
                val index = fileName.indexOf(part, startIndex)
                if (index < 0) return false
                startIndex = index + part.length
            }
            return true
        }

        walkThroughPath(rootPath, maxDepth) { path ->
            if (path.isDirectory()) {
                if (includeDirectories && matches(path.name)) {
                    results.add(path)
                }
            } else if (matches(path.name)) {
                results.add(path)
            }
        }

        return results.stream()
    }

    /**
     * Check whether an asset path exists.
     * @param assetManager Android asset manager.
     * @param assetPath Asset path.
     * @return `true` when asset can be opened.
     */
    fun assetExists(assetManager: AssetManager, assetPath: String): Boolean {
        return try {
            assetManager.open(assetPath).use { true }
        } catch (_: IOException) {
            false
        }
    }

    /**
     * Resolve a relative path case-insensitively under a base directory.
     * @param baseDir Base directory.
     * @param relativePath Relative path.
     * @return Matched file path or `null`.
     */
    fun findFileCaseInsensitive(baseDir: File, relativePath: String): File? {
        val segments = relativePath.replace('\\', '/').split('/').filter { it.isNotEmpty() }
        var current = baseDir
        for (segment in segments) {
            val match = current.listFiles()?.firstOrNull { it.name.equals(segment, ignoreCase = true) } ?: return null
            current = match
        }
        return current.takeIf { it.exists() }
    }
}
