# Cache Controller Design

> **Status**: ARCH-042 Complete
> **Last Updated**: 2026-04-07

## Overview

This document defines the cache key derivation, invalidation policies, and garbage collection strategy for the GameGrub runtime architecture.

---

## Cache Types

### 1. Shader Cache

**Purpose**: Compiled shader state (Mesa, DXVK, VKD3D)

**Key Components**:
- `baseId`: Base bundle version
- `runtimeId`: Wine/Proton version
- `driverId`: Graphics driver version

**Example Key**: `base-linux-glibc2.35-2.35-wine-8.0-glibc2.35-turnip-2024-03-01`

**Storage Location**: `{cache_dir}/shader/{key}/`

### 2. Translator Cache

**Purpose**: Translated code (Box86/Box64/FEXCore)

**Key Components**:
- `baseId`: Base bundle version
- `runtimeId`: Wine/Proton version
- `profileId`: Launch profile
- `exeHash`: Hash of executable

**Example Key**: `base-linux-glibc2.35-2.35-wine-8.0-glibc2.35-wine-modern-abc123`

**Storage Location**: `{cache_dir}/translator/{key}/`

### 3. Probe Cache

**Purpose**: Function resolution and library probing

**Key Components**:
- `baseId`: Base bundle version
- `runtimeId`: Wine/Proton version

**Example Key**: `base-linux-glibc2.35-2.35-wine-8.0-glibc2.35`

**Storage Location**: `{cache_dir}/probe/{key}/`

### 4. Session/Temp Cache

**Purpose**: Temporary session data

**Key Components**:
- Session ID
- Container ID

**Storage Location**: `{cache_dir}/session/{session_id}/`

---

## Key Derivation

### API Reference

**Location**: `app/src/main/java/app/gamegrub/cache/CacheKeyDerivation.kt`

```kotlin
// Generic key derivation
CacheKeyDerivation.deriveKey(
    baseId: String,
    runtimeId: String,
    driverId: String? = null,
    profileId: String? = null,
    exeHash: String? = null,
): CacheKey

// Specialized key derivation
CacheKeyDerivation.deriveShaderCacheKey(
    baseId: String,
    runtimeId: String,
    driverId: String,
): CacheKey

CacheKeyDerivation.deriveTranslatorCacheKey(
    baseId: String,
    runtimeId: String,
    profileId: String,
    exeHash: String,
): CacheKey

CacheKeyDerivation.deriveProbeCacheKey(
    baseId: String,
    runtimeId: String,
): CacheKey
```

### Key Format

```
{baseId}-{runtimeId}-{driverId}-{profileId}-{exeHash}
```

Components are only included if non-null.

---

## Invalidation Policies

### Policy Types

1. **TimeBased**: Invalidate after max age
   ```kotlin
   InvalidationPolicy.TimeBased(maxAgeMs = 30L * 24 * 60 * 60 * 1000) // 30 days
   ```

2. **SizeBased**: Invalidate when exceeding size
   ```kotlin
   InvalidationPolicy.SizeBased(maxSizeBytes = 2L * 1024 * 1024 * 1024) // 2GB
   ```

3. **VersionBased**: Invalidate when version mismatches
   ```kotlin
   InvalidationPolicy.VersionBased(expectedVersion = "1.10")
   ```

4. **Manual**: Always invalidate (requires explicit action)
   ```kotlin
   InvalidationPolicy.Manual
   ```

5. **Never**: Never auto-invalidate
   ```kotlin
   InvalidationPolicy.Never
   ```

### Default Policies

| Cache Type | Default Policy | Max Age |
|-----------|---------------|---------|
| Shader Cache | TimeBased | 30 days |
| Translator Cache | TimeBased | 90 days |
| Probe Cache | SizeBased | 500MB |
| Session Cache | Manual | N/A |

---

## Garbage Collection

### GC Strategy

**Location**: `CacheGarbageCollector`

```kotlin
class CacheGarbageCollector(
    maxTotalSizeBytes: Long = 5L * 1024 * 1024 * 1024,  // 5GB default
    maxAgeMs: Long = 30L * 24 * 60 * 60 * 1000,          // 30 days
)
```

### GC Algorithm

1. **Age-based eviction**: Remove caches older than `maxAgeMs`
2. **Size-based eviction**: If total > `maxTotalSizeBytes`, evict oldest until under limit
3. **Safe eviction**: Never evict active session caches

### GC Result

```kotlin
data class GcResult(
    val evictedCaches: List<CacheManifest>,
    val freedBytes: Long,
    val remainingSizeBytes: Long,
)
```

---

## Cache Manifest

Each cache directory contains a `manifest.json`:

```kotlin
data class CacheManifest(
    val id: String,
    val cacheType: CacheType,
    val createdAt: Long,
    val lastAccessed: Long,
    val baseId: String?,
    val runtimeId: String?,
    val driverId: String?,
    val profileId: String?,
    val exeHash: String?,
    val sizeBytes: Long,
    val path: String,
    val metadata: Map<String, String>,
)
```

---

## Usage Patterns

### Creating Cache Directory

```kotlin
val key = CacheKeyDerivation.deriveShaderCacheKey(
    baseId = "base-linux-glibc2.35-2.35",
    runtimeId = "wine-8.0-glibc2.35",
    driverId = "turnip-2024-03-01",
)
val cacheDir = File(rootDir, "cache/shader/${key.toPathString()}")
```

### Checking Invalidation

```kotlin
val policy = InvalidationPolicy.TimeBased(30 * 24 * 60 * 60 * 1000L)
val shouldInvalidate = CacheInvalidationPolicy.shouldInvalidate(manifest, policy)
```

### Running GC

```kotlin
val gc = CacheGarbageCollector(maxTotalSizeBytes = 5L * 1024 * 1024 * 1024)
val result = gc.runGc(listOfCaches)
println("Freed ${result.freedBytes} bytes")
```

---

## Principles

1. **Deterministic keys**: Same configuration always produces same key
2. **Versioned**: Keys include version identifiers
3. **Safe deletion**: Cache can always be deleted without breaking bundles
4. **Separated**: Each cache type has separate directory and policy
5. **GC-safe**: Garbage collection preserves active session caches

---

## Related Tickets

- **ARCH-043**: CacheController service implementation
- **ARCH-036**: CacheManifest model
- **ARCH-038**: RuntimeStore schema