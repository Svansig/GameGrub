# Launch Failure Taxonomy

> **Status**: ARCH-032 Complete
> **Last Updated**: 2026-04-07

## Overview

This document defines the classification of launch failures, detection strategies, and safe recovery actions for the GameGrub runtime architecture.

---

## Failure Classes

### 1. PROCESS_SPAWN

**Description**: Unable to start the game process (Wine/Proton).

**Detection Strategy**:
- Exit code 127 or "no such file or directory" in stderr
- Process fails to start within timeout
- Process exits immediately without output

**Safe Recovery Actions**:
- `RETRY_SAME_CONFIG`: Retry launch with same configuration
- `RE_EXTRACT`: Re-extract runtime bundles if files are missing

**Exit Code Patterns**:
| Code | Meaning |
|------|---------|
| 127 | Command not found |
| 126 | Permission denied |
| 2 | Missing argument or invalid option |

---

### 2. BACKEND_INIT

**Description**: Wine/Proton startup failure after process spawn.

**Detection Strategy**:
- Exit code 1 with wine-related errors in stderr
- "wineserver not found" in stderr
- Segfault or signal 11
- Timeout during initialization (but not full timeout)

**Safe Recovery Actions**:
- `CACHE_INVALIDATION`: Clear shader/translator caches
- `CONTAINER_RESET`: Reset container to defaults
- `FALLBACK_RUNTIME`: Try alternative Wine/Proton version

**Stderr Patterns**:
```
wineserver not found
 wineserver: could not start
wine: could not start
signal 11 (Segmentation fault)
```

---

### 3. GRAPHICS_INIT

**Description**: Graphics driver or rendering initialization failure.

**Detection Strategy**:
- Vulkan/DirectX errors in stderr
- DXVK/VKD3D initialization failures
- Driver not found errors

**Safe Recovery Actions**:
- `FALLBACK_DRIVER`: Try alternative graphics driver
- `CACHE_INVALIDATION`: Clear shader cache
- `FALLBACK_RUNTIME`: Try Wine with different graphics settings

**Stderr Patterns**:
```
vulkan: failed to create instance
DXVK: failed to initialize
Failed to load driver
renderer: gl: failed to initialize
```

---

### 4. CONTAINER_SETUP

**Description**: Wine prefix, home directory, or mount setup error.

**Detection Strategy**:
- Prefix-related errors in stderr
- Permission errors
- Missing directory errors

**Safe Recovery Actions**:
- `CONTAINER_RESET`: Reset container to defaults
- `RE_EXTRACT`: Re-extract container pattern files

**Stderr Patterns**:
```
Prefix: could not find
Permission denied
/drive_c: No such file or directory
```

---

### 5. MISSING_DRIVER

**Description**: Graphics driver or Wine runtime not found/installed.

**Detection Strategy**:
- "cannot find" in stderr for wine/proton
- Driver not found errors

**Safe Recovery Actions**:
- `RE_EXTRACT`: Re-download and install runtime/driver
- `FALLBACK_RUNTIME`: Try alternative version

**Stderr Patterns**:
```
wine: cannot find
proton: cannot find
Driver not found
```

---

### 6. CORRUPTED_CACHE

**Description**: Shader cache or translator cache corrupted.

**Detection Strategy**:
- DXVK/VKD3D state cache errors
- Mesa shader cache errors
- Cache read failures

**Safe Recovery Actions**:
- `CACHE_INVALIDATION`: Clear affected caches

**Stderr Patterns**:
```
DXVK: state cache error
libdxvk: failed to load
Mesa: shader cache corrupted
```

---

### 7. TIMEOUT

**Description**: Launch exceeded time limit without completion.

**Detection Strategy**:
- Time to failure > 5 minutes (300 seconds)
- No crash or error output

**Safe Recovery Actions**:
- `RETRY_SAME_CONFIG`: Retry - may have been transient
- `FALLBACK_PROFILE`: Try different launch profile

---

### 8. UNKNOWN

**Description**: Unclassified failure.

**Detection Strategy**:
- No matching patterns from above
- Exit code not in known range

**Safe Recovery Actions**:
- `USER_INTERVENTION_REQUIRED`: Require user intervention
- Collect additional diagnostics

---

## Recovery Action Precedence

When a failure occurs, use this precedence:

1. **Transient issues**: Retry same config first
2. **Cache issues**: Invalidate cache, retry
3. **Missing files**: Re-extract, retry
4. **Driver issues**: Fallback driver, retry
5. **Runtime issues**: Fallback runtime, retry
6. **Container issues**: Reset container, retry
7. **Persistent failures**: User intervention

---

## Structured Record

Each failure should produce a `LaunchFailureRecord`:

```kotlin
data class LaunchFailureRecord(
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val failureClass: FailureClass,
    val detectedRootCause: String? = null,
    val stderrSnippet: String? = null,
    val stdoutSnippet: String? = null,
    val processExitCode: Int? = null,
    val timeToFailureMs: Long? = null,
    val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    val context: Map<String, String> = emptyMap(),
)
```

---

## Implementation

- **Location**: `app/gamegrub/launch/error/LaunchFailureRecord.kt`
- **Tests**: `app/gamegrub/launch/error/LaunchFailureRecordTest.kt`

---

## Related Tickets

- **ARCH-031**: Launch fingerprinting (context for failures)
- **ARCH-033**: Milestone recording (when failures occur)
- **ARCH-042**: Adaptive fallback (Phase 10)
- **ARCH-043**: Cache controller (invalidation)