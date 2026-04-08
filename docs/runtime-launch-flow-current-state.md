# Current Runtime/Container/ImageFS Launch Flow

> **Status**: Draft - ARCH-030
> **Last Updated**: 2026-04-07

## Overview

This document maps the current launch flow from app entry point through container startup, identifying shared mutable state, driver extraction points, and container-specific initialization.

---

## 1. Entry Points

### 1.1 External Intents

**File**: `MainActivity.kt` (root package)
- Deep link handler: `home://gamegrub`
- Action: `app.gamegrub.LAUNCH_GAME`
- Routes to `IntentLaunchManager` for resolution

### 1.2 UI Trigger

**File**: `ui/screen/GameGrubMain.kt`
- User clicks play on a game
- Routes through `handleExternalLaunchSuccess` in `ui/launch/GameLaunchOrchestrator.kt`

### 1.3 Launch Coordinator

**File**: `launch/IntentLaunchManager.kt`
- Resolves external launch requests
- Coordinates with `LaunchRequestManager`

---

## 2. Launch Orchestration Path

### 2.1 Entry: handleExternalLaunchSuccess

**Location**: `ui/launch/GameLaunchOrchestrator.kt:72-94`

```kotlin
fun handleExternalLaunchSuccess(
    context: Context,
    appId: String,
    useTemporaryOverride: Boolean,
    viewModel: MainViewModel,
    setMessageDialogState: (MessageDialogState) -> Unit,
) {
    LaunchRequestManager.markAsExternalLaunch()
    trackGameLaunched(appId)
    viewModel.setLaunchedAppId(appId)
    viewModel.setBootToContainer(false)
    preLaunchApp(...)
}
```

### 2.2 Pre-Launch Pipeline: preLaunchApp

**Location**: `ui/launch/GameLaunchOrchestrator.kt:117-659`

Steps executed in sequence:

1. **Container Resolution** (line 139-144)
   ```kotlin
   val containerManager = ContainerManager(context)
   val container = if (useTemporaryOverride) {
       ContainerUtils.getOrCreateContainerWithOverride(context, appId)
   } else {
       ContainerUtils.getOrCreateContainer(context, appId)
   }
   ```
   - Uses Winlator's `ContainerManager(context)` and `Container` class
   - Creates or retrieves existing container
   - Configuration stored in `ContainerData` data class

2. **Executable Resolution** (line 150-174)
   - Queries store services (Steam/GOG/Epic/Amazon) for game executable
   - Fails fast if no executable found

3. **Manifest Component Installation** (line 176-201)
   - Resolves and installs manifest-provisioned components via `BestConfigService`
   - Uses `ManifestInstaller.installManifestEntry()`

4. **Runtime Payload Downloads** (line 206-295)
   - ImageFS download: `SteamService.downloadImageFs()`
   - Wine patches: `SteamService.downloadImageFsPatches()`
   - Proton extraction: `TarCompressorUtils.extract()` to `/opt/`
   - DRM download: `SteamService.downloadFile("experimental-drm-*.tzst")`
   - Steam client download (if `isLaunchRealSteam`)

5. **Launch Dependencies** (line 311-333)
   ```kotlin
   LaunchDependencies().ensureLaunchDependencies(
       context = context,
       container = container,
       gameSource = gameSource,
       gameId = gameId,
       ...
   )
   ```
   - File: `container/launch/dependency/LaunchDependencies.kt`
   - Handles prerequisites (VC redist, PhysX, OpenAL, etc.)

6. **ImageFS Installation** (line 341-359)
   ```kotlin
   ImageFsInstaller.installIfNeededFuture(context, context.assets, container) { ... }.get()
   ```
   - Extracts Linux userspace skeleton from assets
   - Creates `/home/xuser`, `/opt/wine`, etc.

7. **Container Activation** (line 364)
   ```kotlin
   containerManager.activateContainer(container)
   ```

8. **Cloud Sync** (line 366-478)
   - Steam: `SteamService.beginLaunchApp()`
   - GOG: `GOGService.syncCloudSaves()`
   - Epic: `EpicCloudSavesManager.syncCloudSaves()`
   - Amazon: No cloud sync

9. **Final Launch** (line 657)
   - Calls `onSuccess(context, appId)` which triggers XServerActivity

---

## 3. Container Model

### 3.1 ContainerData

**File**: `com/winlator/container/ContainerData.kt`

Key fields:
- `wineVersion`: e.g., "main", "proton-9.0-arm64ec"
- `containerVariant`: "glibc" or "bionic"
- `dxwrapper`: DXVK/VKD3D selection
- `graphicsDriver`: Driver name
- `emulator`: "box64", "box86", "fexcore"
- `startupSelection`: Which Wine prefix files to extract

### 3.2 Container Creation

**File**: `utils/container/ContainerUtils.kt`

- `getOrCreateContainer()`: Creates new or retrieves existing
- `getOrCreateContainerWithOverride()`: Uses temporary override values
- Container ID encoded with game source prefix (steam_, gog_, epic_, amazon_)

### 3.3 Container Manager

**File**: `container/ContainerManager.kt` (GameGrub interface)
**Implementation**: Winlator's `com.winlator.container.ContainerManager`

---

## 4. ImageFS (Shared Mutable Runtime)

### 4.1 Singleton Access

**File**: `com/winlator/xenvironment/ImageFs.java`

```java
public class ImageFs {
    private static volatile ImageFs INSTANCE;
    
    public static ImageFs find(Context context) {
        synchronized (ImageFs.class) {
            if (INSTANCE == null) {
                INSTANCE = new ImageFs(new File(context.getFilesDir(), "imagefs"));
            }
            return INSTANCE;
        }
    }
}
```

**Root Directory**: `{app_files_dir}/imagefs/`

### 4.2 Directory Structure

| Path | Purpose |
|------|---------|
| `/home/xuser` | Container home directory |
| `/home/xuser/.cache` | Shader/translator caches |
| `/home/xuser/.config` | Wine configuration |
| `/home/xuser/.wine` | Default Wine prefix |
| `/opt/wine` | Default Wine installation |
| `/opt/installed-wine` | User-installed Wine versions |
| `/opt/proton-*` | Extracted Proton versions |
| `/usr/lib` | 64-bit libraries |
| `/usr/lib/arm-linux-gnueabihf` | 32-bit libraries |
| `/storage` | External storage mount point |
| `/tmp` | Temporary files |

### 4.3 Container Home Mapping

**Pattern**: `xuser-{container_id}` symlink to actual directory

**File**: `utils/container/ContainerMigrator.kt:83-134`

```kotlin
val legacyDir = File(homeDir, "xuser-$legacyId")
val newDir = File(homeDir, "xuser-$newContainerId")
val activeSymlink = File(homeDir, "xuser")
```

### 4.4 ImageFS Issues

- **Shared state**: Single `INSTANCE` shared across all containers
- **Mutable**: Wine path, cache paths can be modified at runtime
- **No versioning**: Version tracked in `.winlator/.img_version` but not bundle-based

---

## 5. Driver Extraction and Content Management

### 5.1 ContentsManager

**File**: Winlator `com.winlator.contents.ContentsManager`

Handles:
- Wine/Proton installation profiles
- DXVK/VKD3D installation
- Graphics driver (Turnip/Mesa) management

### 5.2 Graphics Driver Preparation

**File**: `container/launch/prep/GraphicsDriverPreparationCoordinator.kt`

- Extracts and configures graphics drivers based on `container.graphicsDriver`
- Uses `containerManager.extractPattern(wineVersion, ...)` for driver files

### 5.3 Wine System Files

**File**: `container/launch/prep/WineSystemFilesCoordinator.kt`

- Extracts Wine prefix template files
- Applies DXVK/VKD3D via `contentsManager.applyContent(profile)`
- Wine version lookup: `contentsManager.getProfileByEntryName(wineVersion)`

---

## 6. Current Failure Modes

### 6.1 Download Failures

| Failure | Detection | Recovery |
|---------|-----------|----------|
| ImageFS download fails | `isImageFsInstallable()` returns false | Retry download |
| Wine patch download fails | `isFileInstallable("imagefs_patches_gamenative.tzst")` | Retry download |
| Proton extraction fails | Bin directory missing after extract | Re-download and extract |
| DRM payload download fails | `isFileInstallable("experimental-drm-*.tzst")` | Retry download |

### 6.2 Installation Failures

| Failure | Detection | Recovery |
|---------|-----------|----------|
| Launch dependencies fail | Exception in `ensureLaunchDependencies()` | Show error dialog |
| ImageFS install fails | `ImageFsInstaller.installIfNeededFuture().get()` returns false | Show error dialog |
| Pattern extraction fails | Exception in `extractPattern()` | Show error dialog |

### 6.3 Cloud Sync Failures

| Failure | Detection | Recovery |
|---------|-----------|----------|
| Sync conflict | `SyncResult.Conflict` | Show conflict dialog |
| Sync in progress | `SyncResult.InProgress` | Retry with backoff |
| Upload pending | `SyncResult.PendingOperations` | Show pending dialog |
| Generic failure | `SyncResult.UnknownFail` | Show error dialog |

### 6.4 Container Activation Failures

| Failure | Detection | Recovery |
|---------|-----------|----------|
| Container not found | `containerManager.getContainer(appId)` returns null | Recreate container |
| Prefix corruption | Wine fails to start | Offer reset to defaults |

---

## 7. Key Files Reference

| File | Role |
|------|------|
| `MainActivity.kt` | Entry point, intent handling |
| `ui/launch/GameLaunchOrchestrator.kt` | Launch orchestration, pre-launch pipeline |
| `launch/IntentLaunchManager.kt` | External intent resolution |
| `utils/container/ContainerUtils.kt` | Container creation/retrieval |
| `com/winlator/container/ContainerData.kt` | Container configuration model |
| `com/winlator/xenvironment/ImageFs.java` | Shared runtime singleton |
| `com/winlator/xenvironment/ImageFsInstaller.java` | Runtime installation |
| `container/manager/WinlatorContainerRuntimeManager.kt` | Runtime operations wrapper |
| `container/launch/dependency/LaunchDependencies.kt` | Prerequisite handling |
| `container/launch/prep/GraphicsDriverPreparationCoordinator.kt` | Driver extraction |
| `container/launch/prep/WineSystemFilesCoordinator.kt` | Wine prefix setup |
| `service/steam/SteamService.kt` | Steam-specific downloads |
| `storage/StorageManager.kt` | File operations, markers |

---

## 8. Current Assumptions About Mutable State

1. **ImageFs is singleton**: All containers share same ImageFs instance
2. **Wine path is mutable**: `imageFs.setWinePath()` called per container
3. **Cache directories are shared**: `.cache`, `.config` not isolated per container
4. **Prefix is extracted per launch**: `extractPattern()` called each launch
5. **No explicit bundle versioning**: Version file exists but not bundle-based

---

## 9. Child Tickets

- **ARCH-031**: Add structured launch fingerprinting and telemetry hooks
- **ARCH-032**: Define launch failure taxonomy and recovery phases
- **ARCH-033**: Define milestones and structured outcome recording
- **ARCH-040**: Design ContainerStore directory schema (Phase 3)
- **ARCH-041**: Implement ContainerStore service (Phase 3)