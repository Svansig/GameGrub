# Runtime Store Directory Schema

> **Status**: ARCH-038 Complete
> **Last Updated**: 2026-04-07

## Overview

This document defines the directory schema for storing immutable runtime bundles (bases, runtimes, drivers) in the GameGrub runtime architecture.

---

## Directory Structure

```
{app_files_dir}/
└── runtime-store/
    ├── bases/
    │   ├── {base-id}/
    │   │   ├── manifest.json          # BaseManifest
    │   │   └── rootfs/                # Linux userspace skeleton
    │   │       ├── etc/
    │   │       ├── usr/
    │   │       ├── lib/
    │   │       └── ...
    │   └── ...
    ├── runtimes/
    │   ├── {runtime-id}/
    │   │   ├── manifest.json          # RuntimeManifest
    │   │   ├── bin/                   # Wine/Proton binaries
    │   │   ├── lib/                   # Wine/Proton libraries
    │   │   └── share/                 # Wine/Proton share files
    │   └── ...
    ├── drivers/
    │   ├── {driver-id}/
    │   │   ├── manifest.json          # DriverManifest
    │   │   └── vulkan/                # Driver files (spirv, etc)
    │   │       ├── icd.d/
    │   │       └── ...
    │   └── ...
    └── index/
        ├── bases.json                 # Index of all bases (optional)
        ├── runtimes.json              # Index of all runtimes (optional)
        └── drivers.json               # Index of all drivers (optional)
```

---

## Bundle Directories

### Base Bundle

**Purpose**: Immutable Linux userspace skeleton (glibc/bionic, system libraries)

**Contents**:
- `manifest.json`: BaseManifest with id, version, contentHash, rootfsPath
- `rootfs/`: Linux directory tree

**Example**:
```
runtime-store/bases/base-linux-glibc2.35-2.35/
├── manifest.json
└── rootfs/
    ├── etc/
    ├── usr/
    │   ├── bin/
    │   └── lib/
    └── ...
```

### Runtime Bundle

**Purpose**: Wine, Proton, or translator runtime

**Contents**:
- `manifest.json`: RuntimeManifest with id, version, contentHash, runtimePath
- `bin/`: Executable binaries (wine, wineserver, etc.)
- `lib/`: Shared libraries
- `share/`: Locale, fonts, etc.

**Example**:
```
runtime-store/runtimes/wine-8.0-glibc2.35/
├── manifest.json
├── bin/
│   ├── wine
│   ├── wineserver
│   └── wineserver64
├── lib/
│   ├── libwine.so
│   └── ...
└── share/
```

### Driver Bundle

**Purpose**: Graphics driver (Vulkan ICDs, Mesa, Turnip, etc.)

**Contents**:
- `manifest.json`: DriverManifest with id, version, contentHash, driverPath
- `vulkan/`: Vulkan ICD files

**Example**:
```
runtime-store/drivers/turnip-merged-2024-03-01/
├── manifest.json
└── vulkan/
    └── icd.d/
        └── turnip.json
```

---

## API Reference

**Location**: `app/src/main/java/app/gamegrub/runtime/store/RuntimeStoreSchema.kt`

### Key Functions

```kotlin
// Directory paths
RuntimeStoreLayout.basesDir(rootDir)
RuntimeStoreLayout.runtimesDir(rootDir)
RuntimeStoreLayout.driversDir(rootDir)

// Bundle paths
RuntimeStoreLayout.baseDir(rootDir, baseId)
RuntimeStoreLayout.runtimeDir(rootDir, runtimeId)
RuntimeStoreLayout.driverDir(rootDir, driverId)

// Manifest paths
RuntimeStoreLayout.baseManifestPath(rootDir, baseId)
RuntimeStoreLayout.runtimeManifestPath(rootDir, runtimeId)
RuntimeStoreLayout.driverManifestPath(rootDir, driverId)

// Schema operations
RuntimeStoreSchema.verifyDirectoryStructure(rootDir): Boolean
RuntimeStoreSchema.createDirectoryStructure(rootDir): Boolean
RuntimeStoreSchema.ensureInitialized(rootDir): Boolean
```

### Bundle Operations

```kotlin
// Write manifest
RuntimeStoreSchema.writeManifest(manifest, rootDir): Boolean
RuntimeStoreSchema.writeManifest(manifest, rootDir): Boolean
RuntimeStoreSchema.writeManifest(manifest, rootDir): Boolean

// Read manifest
RuntimeStoreSchema.readBaseManifest(rootDir, baseId): BaseManifest?
RuntimeStoreSchema.readRuntimeManifest(rootDir, runtimeId): RuntimeManifest?
RuntimeStoreSchema.readDriverManifest(rootDir, driverId): DriverManifest?

// List bundles
RuntimeStoreSchema.listBases(rootDir): List<BaseManifest>
RuntimeStoreSchema.listRuntimes(rootDir): List<RuntimeManifest>
RuntimeStoreSchema.listDrivers(rootDir): List<DriverManifest>

// Delete bundles
RuntimeStoreSchema.deleteBase(rootDir, baseId): Boolean
RuntimeStoreSchema.deleteRuntime(rootDir, runtimeId): Boolean
RuntimeStoreSchema.deleteDriver(rootDir, driverId): Boolean
```

---

## Migration from Legacy ImageFS

The legacy ImageFS uses a shared mutable structure:
```
{data}/files/imagefs/
├── home/xuser/
├── opt/wine/
├── usr/lib/
└── .winlator/
```

The new RuntimeStore is designed to:
1. Co-exist with existing ImageFS during migration
2. Store new bundles in immutable directories
3. Provide verification via contentHash
4. Enable safe deletion without affecting other bundles

---

## Principles

1. **Immutable bundles**: Once installed, bundles are never modified
2. **Versioned**: Each bundle has unique id + version
3. **Verified**: contentHash ensures integrity
4. **Separated**: bases, runtimes, drivers in separate directories
5. **Delete-safe**: Caches can be deleted; bundles must be explicitly removed

---

## Related Tickets

- **ARCH-039**: RuntimeStore service for bundle registration
- **ARCH-037**: Manifest serialization
- **ARCH-034**: Base/Runtime manifest models