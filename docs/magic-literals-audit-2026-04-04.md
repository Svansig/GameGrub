# Magic Numbers and Strings Audit (2026-04-04)

## Scope and method

- Scanned Kotlin sources in `app/src/main/java/app/gamegrub/**` for hardcoded:
  - user-facing strings (`SnackbarManager.show("...")`, `Text("...")`, dialog/status text)
  - numeric literals in control/timing/layout paths (`delay(...)`, retry caps, timeout values, `dp`/`zIndex`)
  - URLs, route/action identifiers, MIME/encoding strings
- Spot-checked `app/src/main/java/com/winlator/**` to separate protocol/domain literals from true code smells.
- Focused on maintainability and localization risk, not on eliminating every literal.

## Findings and handling guidance

### 1) User-facing text embedded in code (high priority)

These should move to string resources for localization and consistency.

Examples:
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:822`
  - `"Downloading update..."`
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:951`
  - `"Failed to submit feedback"`
- `app/src/main/java/app/gamegrub/ui/screen/settings/SettingsGroupDebug.kt:113`
  - `"Failed to save logcat to destination"`
- `app/src/main/java/app/gamegrub/ui/screen/settings/SettingsGroupDebug.kt:165`
  - `"Failed to save Wine log to destination"`
- `app/src/main/java/app/gamegrub/ui/screen/settings/SettingsGroupDebug.kt:267`
  - `"Long click to activate"` (repeated)
- `app/src/main/java/app/gamegrub/ui/screen/settings/DriverManagerDialog.kt:265`
  - `"Import a custom graphics driver package"`
- `app/src/main/java/app/gamegrub/ui/screen/settings/DriverManagerDialog.kt:277`
  - `"Loading available drivers..."`
- `app/src/main/java/app/gamegrub/ui/feedback/GameFeedbackCoordinator.kt:57`
  - `"Thank you for your feedback!"`

Handle by:
- Move to `app/src/main/res/values/strings.xml`.
- Use `stringResource(...)` in Compose and `context.getString(...)` elsewhere.
- Keep placeholders in resources for dynamic pieces (`%1$s`, `%1$d`).

### 2) Repeated URL and external endpoint literals (high priority)

Several URLs are duplicated or declared ad hoc; these are config-like constants and should be centralized.

Examples:
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:552`
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:959`
- `app/src/main/java/app/gamegrub/ui/screen/library/components/SystemMenu.kt:590`
- `app/src/main/java/app/gamegrub/ui/component/dialog/ProfileDialog.kt:157`
  - Repeated Discord invite URL
- `app/src/main/java/app/gamegrub/ui/screen/settings/WineProtonManagerDialog.kt:1089`
  - Manifest URL in function-local `val`
- `app/src/main/java/app/gamegrub/ui/screen/settings/DriverManagerDialog.kt:122`
  - Manifest URL in function-local `val`

Handle by:
- Define in a shared constants owner (for example `Constants.Links.DISCORD_INVITE`, `Constants.Api.*`).
- Prefer one source of truth per endpoint family (api, download, docs/support links).
- If environment-specific, load from build config rather than hardcoding per call site.

### 3) Magic timing/retry numbers in behavior logic (medium-high priority)

These affect UX and reliability but are difficult to tune when inline.

Examples:
- `app/src/main/java/app/gamegrub/ui/model/MainViewModel.kt:328`
  - `while (seconds < 30) { delay(1000) ... }`
- `app/src/main/java/app/gamegrub/ui/model/MainViewModel.kt:464`
  - `delay(100)`
- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt:492`
  - retry cap `5`
- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt:494`
  - `delay(2000)`
- `app/src/main/java/app/gamegrub/ui/screen/library/LibraryScreen.kt:399`
  - `delay(150)`
- `app/src/main/java/app/gamegrub/ui/screen/library/LibraryScreen.kt:429`
  - `delay(32)` (frame-ish backoff)
- `app/src/main/java/app/gamegrub/ui/screen/library/LibraryScreen.kt:495`
  - `delay(50)`
- `app/src/main/java/app/gamegrub/ui/screen/login/UserLoginScreen.kt:601`
  - `delay(3000)`

Handle by:
- Extract to named constants near owner (or a small timing policy object per subsystem).
- Use units in names, for example `CONNECTION_TIMEOUT_SECONDS`, `FOCUS_RETRY_DELAY_MS`.
- For retry loops, extract both cap and interval to keep behavior explicit and testable.

### 4) UI layout constants without design tokens (medium priority)

Some literals are acceptable locally; repeated values should become tokens.

Examples:
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:963`
  - `zIndex(10f)`
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:977`
  - `zIndex(5f)`
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:1252`
  - `RoundedCornerShape(24.dp)`
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:1258`
  - `padding(horizontal = 24.dp, vertical = 12.dp)`

Handle by:
- Keep one-off visual tweaks inline.
- Promote repeated spacing/shape/elevation to shared UI tokens (for example in `ui/theme` or component-level constants).

### 5) MIME, encoding, and protocol literals (medium priority)

These are domain/protocol literals; repeated ones should be deduplicated but not moved to user-facing resources.

Examples:
- `app/src/main/java/app/gamegrub/ui/GameGrubMain.kt:578`
  - `"text/plain"`
- `app/src/main/java/app/gamegrub/service/amazon/AmazonAuthClient.kt:80`
  - `"application/json"` headers
- `app/src/main/java/app/gamegrub/service/gog/api/GOGManifestParser.kt:504`
  - `"UTF-8"`

Handle by:
- Keep protocol values as constants in the owning class/object.
- Reuse existing constants where present (for example `JSON_MEDIA_TYPE` patterns already used in some files).

## What is acceptable and should usually remain literal

- Platform/protocol identifiers that are inherently fixed:
  - Intent action/extras and route templates when already centralized, e.g. `app/src/main/java/app/gamegrub/utils/general/IntentLaunchManager.kt:23`.
- Domain identifiers and enumerations where literal values are the data model:
  - `app/src/main/java/com/winlator/core/envvars/EnvVarInfo.kt:10` and related entries.
- Test-only literals for readability in unit tests (not part of this production scan).

## Recommended rollout

1. **Localization pass first**: move hardcoded user text to `strings.xml` in `ui/*` and coordinators.
2. **Link/API constants pass**: centralize Discord + manifest/API endpoints.
3. **Timing policy pass**: extract retry/delay/timeout numbers into named constants.
4. **UI token pass**: normalize repeated spacing/shape/z-index values where reuse is clear.

## Documentation impact

- Added this audit report to document where magic literals currently exist and how to normalize them safely during refactor work.

