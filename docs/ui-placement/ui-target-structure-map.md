# UI Target Structure Map

This map defines where UI concerns belong and what should not live in each layer.

## Current vs Target (Focused Areas)

| Area | Current Pattern | Target Pattern |
|---|---|---|
| `ui/screen/**` | Mixed rendering + orchestration + service calls in some screens | Rendering/state wiring only; user actions forwarded to ViewModel |
| `ui/model/**` | Existing ViewModels for several screens | Expand as canonical action/state boundary for all UI flows |
| `ui/utils/**` | Contains UI helpers, plus some service-touching helpers | Keep UI-only helpers; move business/auth orchestration out |
| `ui/internal/**` | Preview fake data in main source set | Keep only runtime-safe internals; prefer preview/test source sets for fake fixtures |
| `GameGrubMain` | Large composable with routing + orchestration + launch workflow | Keep nav + high-level UI event handling; delegate launch/session logic to coordinator/use-cases |

## Placement Rules

### 1) `ui/screen/**`

Allowed:
- Composables, local UI state, focus/input handling, navigation callbacks.
- Observing ViewModel state and sending UI intents.

Not allowed:
- Direct service invocation for auth/download/install/sync side effects.
- Long-lived unmanaged coroutine scopes created in UI.

### 2) `ui/model/**`

Allowed:
- UI-facing state models and intents.
- Lifecycle-aware orchestration entry points (`viewModelScope`).
- Calls into services/use-cases/repositories.

Not allowed:
- Compose UI primitives and rendering code.

### 3) `ui/component/**`

Allowed:
- Reusable visual components and callback contracts.

Not allowed:
- Business flow decisions, service state ownership.

### 4) `ui/utils/**`

Allowed:
- Pure UI helpers (formatting, rendering adapters, UI-only wrappers).

Not allowed:
- Platform auth/service state mutations except as transitional wrappers with deprecation plan.

## Explicit Exceptions

The following are intentionally hybrid for now and must be treated as controlled exceptions:

- `app/src/main/java/app/gamegrub/ui/screen/xserver/**`: deep integration with `com.winlator` runtime.
- `app/src/main/res/layout/*.xml` consumed by `com.winlator/**`: legacy view stack boundary.

Exception policy:
- No new app-level feature logic should be added there unless unavoidable.
- New behavior should be isolated behind coordinator/helper classes to keep migration possible.

## Naming and Path Consistency

Canonical active paths:
- `ui/component/` (not `ui/components/`)
- `ui/utils/` (not `ui/util/`)

When updating architecture docs or code comments, use these active paths to prevent drift.

