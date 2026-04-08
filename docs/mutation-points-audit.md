# Mutation Points Audit

> **Status**: ARCH-068 Complete
> **Last Updated**: 2026-04-07

## Overview

This document identifies the remaining shared runtime mutation points that need migration to the new bundle/session architecture.

---

## Identified Mutation Points

### 1. ImageFs Singleton

**Location**: `com/winlator/xenvironment/ImageFs.java`

**Current Behavior**:
```java
private static volatile ImageFs INSTANCE;
public static ImageFs find(Context context) {
    synchronized (ImageFs.class) {
        if (INSTANCE == null) {
            INSTANCE = new ImageFs(new File(context.getFilesDir(), "imagefs"));
        }
        return INSTANCE;
    }
}
```

**Issue**: Single INSTANCE shared across all containers

**Migration**: Replace with per-session mount plan from SessionPlan

---

### 2. Wine Path Mutation

**Location**: `com/winlator/xenvironment/ImageFs.java`

**Current Behavior**:
```java
public void setWinePath(String winePath) {
    this.winePath = winePath;
}
```

**Issue**: Mutated at runtime per launch

**Migration**: Use `envPlan.wineRuntime` from SessionPlan

---

### 3. Cache Directory Sharing

**Location**: `ImageFs.java` - `cache_path`, `config_path`

**Current Behavior**:
```java
public final String cache_path;
public final String config_path;
```

**Issue**: Not isolated per container

**Migration**: Use `mountPlan.containerCacheMount` from SessionPlan

---

### 4. Pattern Extraction Per Launch

**Location**: `WineSystemFilesCoordinator.kt` - `extractPattern()`

**Current Behavior**:
```kotlin
containerManager.extractPattern(container.wineVersion, contentsManager, container.rootDir, null)
```

**Issue**: Re-extracts files each launch

**Migration**: Use cached bundles from RuntimeStore

---

## Migration Status

| Mutation Point | Status | Replacement |
|---------------|--------|-------------|
| ImageFs singleton | To migrate | SessionPlan mount points |
| Wine path mutation | To migrate | envPlan.wineRuntime |
| Cache directory sharing | To migrate | mountPlan.containerCacheMount |
| Pattern extraction | To migrate | RuntimeStore bundle lookup |

---

## Related Tickets

- **ARCH-069**: Replace mutation with bundle/session composition
- **ARCH-038**: RuntimeStore directory schema
- **ARCH-046**: SessionAssembler service