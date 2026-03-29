# Code Quality Improvement Plan

> Focus: Clarity, Readability, Structure, Organization, Correctness, Performance

---

## Current State Analysis

### File Size Issues (Lines of Code)
| File | Lines | Priority | Issue |
|------|-------|----------|-------|
| `XServerScreen.kt` | 4,586 | High | Monolithic UI screen |
| `SteamService.kt` | 3,798 | High | God service class |
| `GameGrubMain.kt` | 2,008 | High | God composable |
| `QuickMenu.kt` | 1,619 | Medium | Complex UI component |
| `GOGDownloadManager.kt` | 1,496 | Medium | Complex download logic |
| `SteamUtils.kt` | 1,403 | Medium | Mixed responsibilities |

### Code Smells Detected
- **14 @Suppress annotations** hiding warnings instead of fixing them
- **111 complex when expressions** across 57 files
- **Duplicate service patterns** across Steam/GOG/Epic/Amazon

---

## Phase 1: Service Layer Decomposition (High Impact)

### SteamService.kt (3798 lines) → 6 Focused Managers

**Current Structure:**
```
SteamService.kt
├── Authentication logic
├── Library sync
├── Cloud saves
├── Achievements
├── Friends/Chat
├── Controller input
└── License management
```

**Target Structure:**
```
service/steam/
├── SteamService.kt           # Core coordinator (~200 lines)
├── SteamAuthService.kt       # Authentication & session
├── SteamLibraryManager.kt    # Game library sync
├── SteamCloudSavesManager.kt # Cloud save handling
├── SteamAchievementManager.kt # Achievement tracking
├── SteamFriendsManager.kt    # Friends & chat
└── SteamLicenseManager.kt    # License management
```

**Extraction Strategy:**
1. Identify cohesive methods grouping by responsibility
2. Extract to new manager with proper DI
3. SteamService delegates to managers
4. Update tests

---

### GOG/Epic/Amazon Services → Same Pattern

Apply same decomposition to:
- `GOGDownloadManager.kt` (1496 lines)
- `EpicCloudSavesManager.kt` (1399 lines)
- `EpicDownloadManager.kt` (1091 lines)

Create consistent manager pattern across all platforms:
```
service/{platform}/
├── {Platform}Service.kt          # Coordinator
├── {Platform}AuthManager.kt      # Authentication
├── {Platform}LibraryManager.kt   # Game library
├── {Platform}DownloadManager.kt  # Downloads
└── {Platform}CloudSavesManager.kt # Cloud saves
```

---

## Phase 2: UI Decomposition (High Impact)

### GameGrubMain.kt (2008 lines) → Modular Composables

**Current Issues:**
- All app state in one file
- Mixed concerns (navigation, state, UI)
- Hard to test and maintain

**Target Structure:**
```
ui/
├── GameGrubApp.kt              # App entry point
├── navigation/
│   ├── AppNavigation.kt        # Navigation graph
│   └── NavRoutes.kt            # Route definitions
├── state/
│   ├── AppState.kt             # App-level state
│   └── AppViewModel.kt         # App-level ViewModel
└── components/
    ├── AppScaffold.kt          # Main scaffold
    └── AppBottomBar.kt         # Bottom navigation
```

---

### XServerScreen.kt (4586 lines) → Feature Modules

This is the largest file. Break into:
```
ui/screen/xserver/
├── XServerScreen.kt           # Main screen orchestrator
├── components/
│   ├── XServerControls.kt     # Control buttons
│   ├── XServerDisplay.kt      # Display area
│   ├── XServerSettings.kt     # Settings panel
│   └── XServerToolbar.kt      # Toolbar
├── viewmodel/
│   └── XServerViewModel.kt    # State management
└── utils/
    └── XServerHelpers.kt      # Helper functions
```

---

## Phase 3: Shared Base Classes (Medium Impact)

### Problem: Duplicate Patterns

Each platform service has similar patterns for:
- Authentication flow
- Library sync
- Download management
- Error handling

### Solution: Abstract Base Classes

```
service/base/
├── BaseService.kt             # Common service patterns
├── BaseAuthManager.kt         # Auth flow template
├── BaseDownloadManager.kt     # Download patterns
└── BaseLibraryManager.kt      # Library sync patterns
```

**Example: BaseAuthManager**
```kotlin
abstract class BaseAuthManager {
    abstract suspend fun authenticate(credentials: AuthCredentials): Result<AuthToken>
    abstract suspend fun refreshToken(refreshToken: String): Result<AuthToken>
    abstract fun isAuthenticated(): Boolean
    
    // Common error handling
    protected fun handleAuthError(e: Exception): Result<Nothing> {
        Timber.e(e, "Auth failed")
        return Result.failure(AuthException(e.message ?: "Authentication failed"))
    }
}
```

---

## Phase 4: Error Handling Improvements (Medium Impact)

### Current Issues
- Inconsistent error handling patterns
- Some errors silently swallowed
- No unified error types

### Target: Result Pattern with Custom Errors

```kotlin
// Define domain-specific errors
sealed class GameNativeError : Exception() {
    object AuthenticationRequired : GameNativeError()
    data class NetworkError(val code: Int, override val message: String) : GameNativeError()
    data class StorageError(val path: String, override val message: String) : GameNativeError()
    data class GameLaunchError(val appId: String, override val message: String) : GameNativeError()
}

// Consistent error handling
suspend fun <T> executeWithErrorHandling(
    tag: String,
    block: suspend () -> T
): Result<T> = runCatching {
    block()
}.onFailure { e ->
    Timber.tag(tag).e(e, "Operation failed")
}
```

---

## Phase 5: Code Organization (Low Impact)

### 1. Fix @Suppress Annotations (14 instances)

| File | Annotation | Fix |
|------|------------|-----|
| `PrefManager.kt` | @Suppress("UnsafeOptInUsage") | Proper opt-in declaration |
| `SteamTokenLogin.kt` | @Suppress("BinaryOperationInTimber") | Fix Timber logging |
| `EventDispatcher.kt` | @Suppress("UNCHECKED_CAST") | Add proper type bounds |
| `SteamUtils.kt` | @Suppress("DEPRECATION") | Migrate to new API |

### 2. Improve Naming

- `Net` → `NetworkClient` (more descriptive)
- `PrefManager` → `AppPreferences` (clearer intent)
- `GameGrubMain` → `MainAppContent` (Compose convention)

### 3. Reduce Complexity

- Break long `when` expressions into extension functions
- Extract magic numbers to named constants
- Use `require`/`check` for precondition validation

---

## Implementation Order

| Phase | Effort | Impact | Risk | Status |
|-------|--------|--------|------|--------|
| 1. SteamService decomposition | High | High | Medium | 🔲 |
| 2. GOG/Epic/Amazon services | Medium | High | Medium | 🔲 |
| 3. GameGrubMain decomposition | High | High | High | 🔲 |
| 4. XServerScreen decomposition | High | Medium | High | 🔲 |
| 5. Base class extraction | Medium | Medium | Low | 🔲 |
| 6. Error handling | Low | Medium | Low | 🔲 |
| 7. Code organization | Low | Low | Low | 🔲 |

---

## Testing Strategy

For each decomposition:
1. Write tests BEFORE refactoring (characterization tests)
2. Extract code to new location
3. Verify behavior unchanged via tests
4. Add new unit tests for isolated components

---

## Success Metrics

- [ ] No file over 500 lines
- [ ] No function over 50 lines
- [ ] Cyclomatic complexity < 10 per function
- [ ] 100% of public APIs have KDoc
- [ ] Zero @Suppress annotations (or documented exceptions)
- [ ] All services follow consistent patterns

---

## Quick Wins (Start Here)

1. **Extract SteamService auth logic** (est. 400 lines → SteamAuthService)
2. **Extract GameGrubMain navigation** (est. 300 lines → AppNavigation)
3. **Remove @Suppress annotations** and fix underlying issues
4. **Add KDoc to PrefManager** (critical configuration class)
