# UI Validation Checklist

Use this checklist for each UI placement fix PR.

## 1) Static Quality Gates

- [ ] `./gradlew lintKotlin` passes for touched modules.
- [ ] No new ktlint violations introduced in touched files.
- [ ] Imports and package placement align with target structure map.

## 2) Behavior Preservation

- [ ] Existing user-visible flow remains unchanged unless explicitly scoped.
- [ ] Dialogs/snackbars still appear at expected points.
- [ ] Navigation route outcomes remain equivalent.

## 3) Lifecycle and Scope Safety

- [ ] No new `CoroutineScope(Dispatchers.IO)` in UI screen/component files.
- [ ] Side-effecting operations run through ViewModel/coordinator scope.
- [ ] Cancellation behavior is deterministic on screen exit.

## 4) Architecture Boundary Checks

- [ ] UI file no longer directly invokes service methods for migrated flow.
- [ ] UI receives state/effects from ViewModel rather than deriving service state ad hoc.
- [ ] Any exception boundary is documented in PR and triage register.

## 5) Targeted Runtime Verification

For relevant migrated areas, confirm manually:

- [ ] Login/logout path still functions.
- [ ] Download/install/uninstall actions still function.
- [ ] External launch intent path still functions.
- [ ] Offline/online transition behavior still functions.

## 6) Minimal Command Set

```powershell
./gradlew testDebugUnitTest
./gradlew lintKotlin
```

Use additional module-specific commands as needed for touched area.

## 7) PR Evidence Template

```md
## UI Placement Validation
- Triage IDs: UI-XXX
- Commands run:
  - ./gradlew testDebugUnitTest
  - ./gradlew lintKotlin
- Manual checks:
  - [x] Login flow
  - [x] Download action
  - [x] Navigation return path
- Residual risks:
  - ...
```

## Recent Evidence

### UI-004

- Triage IDs: `UI-004`
- Commands prepared:
  - `./gradlew testDebugUnitTest --tests "app.gamegrub.ui.model.LibraryAuthResultParserTest"`
- Manual checks:
  - [ ] GOG login success/cancel
  - [ ] Epic login success/cancel
  - [ ] Amazon login success/cancel
- Residual risks:
  - Full `lintKotlin` and end-to-end auth device verification still pending.

