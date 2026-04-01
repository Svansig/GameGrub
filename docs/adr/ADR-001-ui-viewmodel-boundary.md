# ADR-001: UI-to-ViewModel Boundary Contract

## Status
Accepted

## Date
2024-01-15

## Context
UI screens (`ui/screen/**`) were directly calling service methods and creating unmanaged coroutine scopes, mixing rendering logic with business orchestration.

## Decision
Enforce strict boundary between UI and ViewModel layers:
- **Allowed in UI**: Composables, local UI state, navigation callbacks, observing ViewModel state
- **Not allowed**: Direct service invocation, `CoroutineScope(Dispatchers.IO).launch`, business flow decisions

## Consequences
- All platform app screens now route operations through ViewModels
- Coroutine lifecycle tied to `viewModelScope` (lifecycle-aware)
- Service calls go through domain interfaces, not static service access

## Related Tickets
- `todo/UI-001.md`, `todo/UI-002.md`, `todo/UI-003.md`, `todo/UI-005.md`

## Related Docs
- `docs/ui-placement/ui-target-structure-map.md`
- `docs/ui-placement/ui-element-triage-register.md`