# ADR-002: Legacy Winlator UI Seam as Controlled Exception

## Status
Accepted

## Date
2024-01-15

## Context
The app has a Compose-first UI (`app.gamegrub.ui`) inherited from Pluvia fork and a legacy XML/View implementation in `com.winlator`. New app features were at risk of bleeding into the legacy stack.

## Decision
Define explicit exception boundary for legacy seam:
- **Allowed exceptions**:
  - `app/src/main/java/app/gamegrub/ui/screen/xserver/**`: deep integration with `com.winlator` runtime
  - `app/src/main/res/layout/*.xml` consumed by `com.winlator/**`: legacy view stack boundary
- **Policy**: No new app-level feature logic should be added there unless unavoidable
- **Migration path**: New behavior should be isolated behind coordinator/helper classes to keep migration possible

## Consequences
- UI placement docs explicitly document this exception boundary
- Refactors targeting UI boundary don't need to address legacy stack
- Clear separation between "in-scope for refactor" and "explicitly excepted"

## Related Tickets
- `todo/UI-009.md`

## Related Docs
- `docs/ui-placement/ui-target-structure-map.md` (Explicit Exceptions section)