# Container Store Directory Schema

> **Status**: ARCH-040 Complete
> **Last Updated**: 2026-04-07

## Overview

This document defines the directory schema for storing mutable per-game container state (prefix, install, saves, overrides, cache) in the GameGrub runtime architecture.

---

## Directory Structure

```
{app_files_dir}/
└── containers/
    ├── {container-id}/
    │   ├── manifest.json              # ContainerManifest
    │   ├── prefix/                    # Wine prefix (mutable)
    │   │   ├── drive_c/
    │   │   ├── users/
    │   │   └── .wine/
    │   ├── install/                  # Game installation (mutable)
    │   │   └── ...
    │   ├── saves/                    # Save files (mutable)
    │   │   └── ...
    │   ├── overrides/                # User overrides (mutable)
    │   │   ├── dlls/
    │   │   └── registry/
    │   └── cache/                    # Container-specific cache (disposable)
    │       ├── shader/
    │       └── translator/
    ├── {container-id-2}/
    │   └── ...
    └── ...
```

---

## Container Directories

### Prefix Directory

**Purpose**: Wine prefix (registry, drive_c, user files)

**Contents**:
- `drive_c/`: Windows C: drive
- `users/`: Windows user directories
- `.wine/`: Wine system files

**Example**:
```
containers/steam_12345/prefix/
├── drive_c/
│   ├── Program Files/
│   └── Program Files (x86)/
├── users/
│   └── xuser/
│       ├── AppData/
│       └── Documents/
└── .wine/
    ├── system.reg
    └── user.reg
```

### Install Directory

**Purpose**: Game installation files

**Contents**:
- Game executables
- Game assets
- DLC

**Example**:
```
containers/steam_12345/install/
├── Game.exe
├── Data/
└── DLC/
```

### Saves Directory

**Purpose**: Save files and cloud sync

**Contents**:
- Save game files
- Profile data

**Example**:
```
containers/steam_12345/saves/
├── save1.sav
└── profile/
```

### Overrides Directory

**Purpose**: User configuration overrides

**Contents**:
- DLL overrides
- Custom registry entries

**Example**:
```
containers/steam_12345/overrides/
├── dlls/
│   └── d3d12.dll
└── registry/
    └── custom.reg
```

### Cache Directory

**Purpose**: Container-specific cache (safe to delete)

**Contents**:
- Shader cache
- Translator cache

**Example**:
```
containers/steam_12345/cache/
├── shader/
│   └── dxvk/
└── translator/
    └── box64/
```

---

## API Reference

**Location**: `app/src/main/java/app/gamegrub/container/store/ContainerStoreSchema.kt`

### Key Functions

```kotlin
// Directory paths
ContainerStoreLayout.containersDir(rootDir)
ContainerStoreLayout.containerDir(rootDir, containerId)
ContainerStoreLayout.prefixDir(rootDir, containerId)
ContainerStoreLayout.installDir(rootDir, containerId)
ContainerStoreLayout.savesDir(rootDir, containerId)
ContainerStoreLayout.overridesDir(rootDir, containerId)
ContainerStoreLayout.cacheDir(rootDir, containerId)
ContainerStoreLayout.containerManifestPath(rootDir, containerId)

// Schema operations
ContainerStoreSchema.verifyDirectoryStructure(rootDir): Boolean
ContainerStoreSchema.createDirectoryStructure(rootDir): Boolean
ContainerStoreSchema.ensureInitialized(rootDir): Boolean
```

### Container Operations

```kotlin
// Write/read manifest
ContainerStoreSchema.writeManifest(manifest, rootDir): Boolean
ContainerStoreSchema.readManifest(rootDir, containerId): ContainerManifest?

// List containers
ContainerStoreSchema.listContainers(rootDir): List<ContainerManifest>

// Delete container
ContainerStoreSchema.deleteContainer(rootDir, containerId): Boolean

// Ensure directories
ContainerStoreSchema.ensureContainerDirectories(rootDir, containerId): Boolean
```

---

## Seeding Strategy

When a new container is created:

1. Create container directories (prefix, install, saves, overrides, cache)
2. Write ContainerManifest with game metadata
3. Seed prefix from template (optional)
4. Link to runtime bundle paths

---

## Principles

1. **Mutable per-game**: Each container is independent
2. **Separated concerns**: Prefix, install, saves, overrides, cache separate
3. **Cache-safe**: Cache directory can be deleted without losing game data
4. **Versioned**: ContainerManifest records gameId, platform, configuration
5. **Migration-ready**: Schema supports migration from legacy paths

---

## Related Tickets

- **ARCH-041**: ContainerStore service for container lifecycle
- **ARCH-036**: ContainerManifest model
- **ARCH-038**: RuntimeStore schema (for bundle references)