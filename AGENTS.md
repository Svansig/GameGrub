# GameNative Developer Guide

This document provides guidelines and commands for agentic coding agents working on the GameNative project.

## Project Overview

GameNative is an Android application (fork of Pluvia) that allows playing Steam, Epic, GOG, and Amazon games on Android devices. It uses:
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **UI**: Jetpack Compose
- **DI**: Hilt
- **Database**: Room
- **Linting**: Kotlinter + ktlint rules
- **Testing**: JUnit 4, Robolectric, MockK, Mockito

## Build Commands

### Gradle Wrapper
All commands use `./gradlew` from the project root.

### Build Variants
- **Debug**: `./gradlew assembleDebug`
- **Release**: `./gradlew assembleRelease`
- **Release Signed**: `./gradlew assembleReleaseSigned`
- **Release Gold**: `./gradlew assembleReleaseGold`

### Running Tests

**Run all unit tests:**
```bash
./gradlew test
```

**Run all unit tests for a specific variant:**
```bash
./gradlew testDebugUnitTest
```

**Run a single test class:**
```bash
./gradlew testDebugUnitTest --tests "app.gamegrub.utils.DateTimeUtilsTest"
```

**Run a single test method:**
```bash
./gradlew testDebugUnitTest --tests "app.gamegrub.utils.DateTimeUtilsTest.parseStoreReleaseDateToEpochSeconds_parsesIsoInstant"
```

**Run tests matching a pattern:**
```bash
./gradlew testDebugUnitTest --tests "*Test"
```

**Run Android Instrumented Tests (on device/emulator):**
```bash
./gradlew connectedDebugAndroidTest
```

### Linting

**Run Kotlinter (lint + formatting):**
```bash
./gradlew lintKotlin        # Lint only
./gradlew formatKotlin      # Format only
./gradlew lintKotlin formatKotlin  # Both
```

**Run Android lint:**
```bash
./gradlew lint
```

### Code Generation

**Generate Room database (after schema changes):**
```bash
./gradlew generateRoomSchema
```

### Other Useful Commands

**Clean build:**
```bash
./gradlew clean
```

**Build with dependencies:**
```bash
./gradlew assembleDebug --refresh-dependencies
```

## Code Style Guidelines

### General Rules

- **Max line length**: 140 characters (from `.editorconfig`)
- **Kotlin code style**: Official (`kotlin.code.style=official` in `gradle.properties`)
- **Encoding**: UTF-8

### EditorConfig Settings (`.editorconfig`)

The project uses these ktlint rules:
- Max line length: 140
- Trailing commas: enabled
- Android Studio code style
- Function naming ignore when annotated with `@Composable`
- These rules are **disabled**:
  - `argument-list-wrapping`
  - `backing-property-naming`
  - `class-signature`
  - `enum-entry-name-case`
  - `function-expression-body`
  - `function-signature`
  - `no-blank-line-in-list`
  - **Wildcard imports are allowed** (`no-wildcard-imports` disabled)
  - `package-name`

### Naming Conventions

- **Classes**: PascalCase (e.g., `GameGrubApp`, `DateTimeUtils`)
- **Functions**: camelCase (e.g., `parseStoreReleaseDateToEpochSeconds`)
- **Properties/Variables**: camelCase
- **Constants**: UPPER_SNAKE_CASE (e.g., `SUSPEND_POLICY_MANUAL`)
- **Object/Companion Object**: PascalCase for objects, use for grouping utilities
- **Package names**: lowercase, no underscores (e.g., `app.gamegrub.utils`)
- **Test classes**: Must end with `Test` (e.g., `DateTimeUtilsTest`)

#### Architecture Role Naming

| Role | Suffix/Pattern | Example |
|------|---------------|---------|
| **Domain** | `Domain` | `SteamInstallDomain`, `GOGAccountDomain` |
| **Manager** | `Manager` | `ContainerManager`, `DownloadManager` - use for orchestration |
| **Coordinator** | Use object/singleton or `Coordinator` suffix | `GameLaunchCoordinator` |
| **Gateway** | `Gateway` | `LaunchRequestGateway`, `PreferencesGateway` - boundaries |
| **UseCase** | `UseCase` | `LaunchGameUseCase`, `SyncCloudSavesUseCase` |

### Imports

- **Order**: No strict order enforced (ktlint rule disabled), but group logically
- **Wildcard imports**: Allowed (disabled rule)
- **Fully qualify** when ambiguous or for clarity

Example:
```kotlin
import android.os.StrictMode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import app.gamegrub.db.dao.AmazonGameDao
import app.gamegrub.db.dao.GOGGameDao
import com.google.android.play.core.splitcompat.SplitCompatApplication
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import javax.inject.Inject
```

### Formatting

- **Semicolons**: Not used
- **Braces**: Always use braces for control flow, even single-line bodies
- **Blank lines**: Use sparingly to separate logical blocks
- **Indentation**: 4 spaces (Android standard)

### Types

- **Explicit types**: Use explicit types for public API; type inference okay for local variables
- **Null safety**: Use Kotlin null safety (`?`, `?:`, `?.`)
- **Collections**: Use Kotlin collections (`List<T>`, `Map<K,V>`), not Java arrays/collections
- **Coroutines**: Use `Dispatchers.IO` for blocking operations, `Dispatchers.Main` for UI

### KDoc and Documentation

- **Public APIs**: Add KDoc for public utility functions, ViewModels, and domain methods
- **Required fields**: `@param`, `@return`, `@throws` for functions with parameters or exceptions
- **Optional**: Brief one-liner description for straightforward methods
- **Example**:
```kotlin
/**
 * Parses a store release date string to epoch seconds.
 * @param dateString ISO-8601 formatted date string (e.g., "2024-01-15T00:00:00Z")
 * @return Epoch seconds, or 0 if parsing fails
 * @throws IllegalArgumentException if dateString is null
 */
fun parseStoreReleaseDateToEpochSeconds(dateString: String?): Long
```

### Error Handling

- **Prefer `runCatching`**: For operations that may fail gracefully
- **Use Timber**: For logging, never use `println()`
- **Don't swallow exceptions silently**: At minimum, log with `Timber.e(e, "message")`
- **Use `getOrDefault` / `getOrElse`**: For recoverable failures

**Error Message Guidelines:**
- Be specific about what went wrong (e.g., "Failed to download game" not "Error occurred")
- Include actionable info when possible (e.g., "Download failed: network unavailable")
- Use consistent voice: short, direct, user-friendly
- Avoid technical jargon in user-facing messages; log details for debugging

Example:
```kotlin
return runCatching {
    // risky operation
}.getOrDefault(defaultValue)
```

### Hilt Dependency Injection

- **Application class**: Annotate with `@HiltAndroidApp`
- **Activities/Fragments**: Use `@AndroidEntryPoint`
- **ViewModels**: Use `@HiltViewModel`
- **Inject**: Use `@Inject lateinit var` for fields
- **Constructor injection**: Preferred when possible

Example:
```kotlin
@HiltAndroidApp
class GameGrubApp : SplitCompatApplication() {
    @Inject
    lateinit var gogGameDao: GOGGameDao
}
```

### Compose

- **Composable functions**: Uppercase names not required (ktlint rule disables this)
- **State**: Use `mutableStateOf` with `by` delegate or explicit `getValue/setValue`
- **Side effects**: Use `LaunchedEffect`, `remember`, `rememberCoroutineScope`

### Room Database

- **DAOs**: Define interfaces with `@Dao` annotation
- **Entities**: Data classes with `@Entity` annotation
- **Type converters**: Use `@TypeConverter` for complex types

### Testing

- **Framework**: JUnit 4 with Robolectric for Android tests
- **Mocking**: MockK for Kotlin-first mocking, Mockito as fallback
- **Assertions**: Use JUnit `Assert` methods or AssertJ-style
- **Test location**: `app/src/test/java/` for unit tests
- **Naming**: `<ClassName>Test` for test class, `<method>_expectedBehavior>` for test methods

Example:
```kotlin
class DateTimeUtilsTest {
    @Test
    fun parseStoreReleaseDateToEpochSeconds_parsesIsoInstant() {
        val input = "2026-02-21T16:12:31Z"
        val expected = Instant.parse(input).epochSecond
        val actual = DateTimeUtils.parseStoreReleaseDateToEpochSeconds(input)
        assertEquals(expected, actual)
    }
}
```

### Logging

- **Use Timber**: Never use `println()` or `android.util.Log`
- **Log levels**:
  - `Timber.d("message")` - Debug, development info
  - `Timber.i("message")` - Info, general operational events
  - `Timber.w(e, "message")` or `Timber.w("message")` - Warning, recoverable issues
  - `Timber.e(e, "message")` - Error, failures that need attention
- **Tag conventions**:
  - Default: Use class name automatically via `Timber.tag("TagName")`
  - Contextual: `[ComponentName][Action] message` for clarity (e.g., `[GameGrubMain][Launch] Starting game`)
  - Avoid: Hard-coded tags per line; use contextual logging or class tags
- **Message format**:
  - Be descriptive but concise
  - Include relevant identifiers (appId, gameId, userId) when available
  - Avoid敏感 data in logs (see `todo/SEC-004.md`)
- **Plant DebugTree in debug builds**:
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
}
```

### Resources

- **Strings**: Use `stringResource()` in Compose, `getString(R.string.xxx)` elsewhere
- **Colors**: Define in `ui/theme/Color.kt`
- **Typography**: Define in `ui/theme/Typography.kt`
- **Themes**: Define in `ui/theme/Theme.kt`

### ProGuard/R8

- Rules are in `app/proguard-rules.pro`
- Keep Hilt, Room, Serialization classes when minifying

## Project Structure

```
app/src/
├── main/
│   ├── java/app/gamegrub/
│   │   ├── db/           # Room database, DAOs, entities
│   │   ├── service/      # Steam, GOG, Epic, Amazon services
│   │   ├── ui/           # Compose screens, components, theme
│   │   ├── utils/        # Utility classes and helpers
│   │   └── ...           # MainActivity, GameGrubApp
│   └── res/              # Android resources
├── test/                 # Unit tests
├── androidTest/          # Instrumented tests
└── sharedTest/           # Shared test code

ubuntufs/src/
├── main/                 # Dynamic feature module resources/code
├── test/                 # Unit tests
└── androidTest/          # Instrumented tests
```

## Common Tasks

### Current Focus (Refactor Phase)
- The current project focus is refactoring the codebase into a more manageable, cohesive, and understandable structure.
- Prefer tasks that improve boundaries, reduce complexity, and improve maintainability/testability of existing behavior.
- Do not expand feature scope unless it is required to complete an active refactor ticket.

### Picking Work (Local Tickets)
1. Open `todo/INDEX.md`
2. Select the highest-priority unblocked ticket
3. Read the ticket file (for example `todo/UI-004.md`) and follow its acceptance criteria
4. Update ticket status as work progresses (`Backlog` -> `In Progress` -> `Done`)
5. Update the ticket's documentation impact and links to updated docs
6. Link validation evidence and PR references in the ticket

### Ticket Decomposition and Continuity Rules
- If a ticket is too complex to complete safely in one pass, create child tickets in `todo/` immediately and link them from the parent ticket.
- If a child ticket is required to complete the current ticket's acceptance criteria, start and work that child ticket immediately.
- If work reveals new required follow-up tasks, create those tickets immediately (do not defer ticket creation).
- Do not abandon a ticket once it is marked `In Progress`; either finish it, or explicitly mark it `Blocked` with a concrete blocker and linked child/follow-up tickets.

### Documentation Directive
- Every change must include documentation updates.
- Each PR must include a `Documentation Impact` note.
- If no docs are changed, include `Documentation Impact: No doc changes required` with a concrete reason.

### Completion and Review Protocol
- When implementation for a ticket is complete, commit the work before requesting review.
- Have a different agent/reviewer perform an independent check.
- Apply review feedback, then confirm all resulting changes are committed.
- Do not mark a ticket `Done` until implementation, review, and post-review commits are complete.
- Log improvement opportunities in `docs/process-improvement-log.md` for both code quality and workflow quality.

### Adding a New Service (Steam/GOG/Epic/Amazon)
1. Create service class in `app/src/main/java/app/gamegrub/service/`
2. Add Hilt module binding if needed
3. Add database migration if storing data
4. Add tests in `app/src/test/java/app/gamegrub/service/`

### Adding a Game Fix
1. Create class implementing `GameFix` interface in `gamefixes/`
2. Register in `GameFixesRegistry`
3. Name format: `<PLATFORM>_<GAMEID>.kt`

### Adding a Database Entity
1. Create data class with `@Entity` in appropriate package
2. Create `@Dao` interface
3. Add to `GameGrubDatabase`
4. Run `./gradlew generateRoomSchema` to generate schema

## Build Configuration

- **Compile SDK**: 35
- **Min SDK**: 33 (Android 13)
- **Target SDK**: 35 (Android 15)
- **Java Version**: 17
- **Kotlin Version**: 2.1.21 (see `gradle/libs.versions.toml`)

## Additional Notes

- Join Discord for development discussions: https://discord.gg/2hKv4VfZfE
- Report issues via Discord, not GitHub
- Test on real device when possible (NDK, native code)
- The app uses native C/C++ code in `app/src/main/cpp/`

## Entry Points

- **Application**: `app.gamegrub.GameGrubApp` (root package) - initializes Timber, NetworkManager, CrashHandler, PrefManager, PostHog, PlayIntegrity
- **Main Activity**: `app.gamegrub.MainActivity` (root package) - handles `home://gamegrub` deep link and `app.gamegrub.LAUNCH_GAME` action
- **OAuth**: `ui/screen/auth/{GOG,Epic,Amazon}OAuthActivity.kt`
- **Services**: SteamService, GOGService, EpicService, AmazonService (foreground)
- **Activity Alias**: Uses `MainActivityAliasDefault`/`MainActivityAliasAlt` for runtime icon switching

## Structure Deviations

- **Legacy code**: `com.winlator/` package (XServer, renderer, container) - inherited from Pluvia fork
- **UI components dir**: `ui/component/` is the active directory; references to `ui/components/` are legacy/outdated
- **Nested utils**: utils are partially reorganized into subpackages (`utils/auth/`, `utils/container/`, `utils/game/`, `utils/general/`, etc.); legacy nested dirs like `utils/launchdependencies/` and `utils/preInstallSteps/` still exist

## CI/Build Gaps

- **No Kotlinter in PR checks**: `pluvia-pr-check.yml` only runs tests, not lint
- **Release uses debug signing**: `app/build.gradle.kts` `release` build type still signs with debug keystore (only `release-signed`/`release-gold` use production keystore)
- **Custom build type**: `release-signed` uses dual signing with lineage for Play Store updates

## Anti-Patterns

- No `DO NOT`/`NEVER`/`ALWAYS` comment-based rules in source code
- `@Deprecated` exists beyond legacy OAuth callback use (for example in `utils/general/NoToast.kt` and `ui/enums/SortOption.kt`)
- All uppercase "NEVER"/"ALWAYS" in code are constants, not anti-pattern comments
