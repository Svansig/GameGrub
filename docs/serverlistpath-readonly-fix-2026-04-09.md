# ServerListPath Read-Only FileSystem Fix - April 9, 2026

## Issue
**Error**: `java.nio.file.FileSystemException: server_list.bin: Read-only file system`

**Root Cause**: `SteamPaths.serverListPath` was hardcoded to use `DownloadService.baseCacheDirPath` which points to `context.cacheDir`. On this Android configuration, the cache directory is read-only, preventing FileServerListProvider from writing the server list.

**Stack Trace**:
```
FileSystemException: server_list.bin: Read-only file system
  at FileServerListProvider.updateServerList()
  at SmartCMServerList.replaceList()
  → Fails to write server cache
```

## Solution
Enhanced `SteamPaths.serverListPath` with priority-based writable path fallbacks:

```kotlin
val serverListPath: String
    get() {
        // Priority 1: Try cache directory first (preferred location)
        val cachePath = DownloadService.baseCacheDirPath
        if (cachePath.isNotBlank() && File(cachePath).let { it.exists() && it.canWrite() }) {
            return Paths.get(cachePath, "server_list.bin").pathString
        }

        // Priority 2: Fall back to data directory (guaranteed writable)
        val dataPath = DownloadService.baseDataDirPath
        if (dataPath.isNotBlank() && File(dataPath).let { it.exists() && it.canWrite() }) {
            Timber.w("Cache directory not writable, using data directory for server list")
            return Paths.get(dataPath, "server_list.bin").pathString
        }

        // Priority 3: Try external app directory if available
        val externalAppPath = DownloadService.baseExternalAppDirPath
        if (externalAppPath.isNotBlank() && File(externalAppPath).let { it.exists() && it.canWrite() }) {
            Timber.w("Using external app directory for server list: $externalAppPath")
            return Paths.get(externalAppPath, "server_list.bin").pathString
        }

        // Final fallback
        return Paths.get(cachePath.takeIf { it.isNotBlank() } ?: "/cache", "server_list.bin").pathString
    }
```

**Key Changes**:
1. ✅ **Checks write permissions** - Not just existence
2. ✅ **Priority 1**: Cache (preferred for transient data)
3. ✅ **Priority 2**: Data directory (guaranteed writable)
4. ✅ **Priority 3**: External app directory
5. ✅ **Logs fallback decisions** for debugging

## Why This Works

- **Cache Directory**: Ideal for server list (transient, refreshed regularly)
- **Data Directory**: Always writable by the app, guaranteed fallback
- **External App Directory**: Writable on most configurations
- **Logging**: Shows which path was chosen for debugging

## Related Fix
This mirrors the fix applied to `SteamPaths.defaultStoragePath` which uses similar fallback logic for storage path resolution.

## Files Modified
1. `/app/src/main/java/app/gamegrub/service/steam/SteamPaths.kt` - Added robust `serverListPath` resolution

## Testing
The error should not occur on next Steam connection attempt. Server list will be cached in a writable location.


