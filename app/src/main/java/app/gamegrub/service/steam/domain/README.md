# Steam Domains

This directory contains the domain layer for Steam integration.

## End Goal

Domains are the business workflow owners for Steam. The target is a thin `SteamService` shell that delegates to domains for all non-Android logic.

In end-state architecture, each domain:
- owns a cohesive workflow area,
- coordinates repositories/DAOs and manager helpers,
- exposes stable methods for service and UI-facing facades,
- is testable without Android service lifecycle coupling.

## Domain Responsibilities

- `SteamAccountDomain`
  - Authentication/login flow orchestration.
  - Persona/friends/account identity behavior.
  - Account runtime state model (login progress, user identity, QR flow state).

- `SteamLibraryDomain`
  - Library/catalog metadata and license persistence workflows.
  - Owned games refresh and app/license mapping.
  - Download state persistence boundaries (`AppInfo`, `DownloadingAppInfo`, cached licenses).

- `SteamPicsSyncDomain`
  - PICS change polling and product-info sync loops.
  - PICS channels/job lifecycle and token-driven batch sync behavior.
  - Emission of metadata updates into library persistence paths.

- `SteamInstallDomain`
  - Install/download planning and executable resolution behavior.
  - Steam Input template routing and config resolution.
  - High-level install orchestration helpers used by download coordinator logic.

- `SteamSessionDomain`
  - App launch/close session orchestration.
  - Running-process notifications and playing-session policy.
  - Session file/ticket lifecycle behavior.

- `SteamCloudStatsDomain`
  - Achievement generation/cache/state transitions.
  - Cloud stats and user file sync orchestration.

## What Belongs Here

- Business rules and flow control.
- Multi-step orchestration that spans DAOs, managers, and external Steam handlers.
- State that must survive beyond a single callback body and is not Android lifecycle wiring.

## What Does Not Belong Here

- Android Service lifecycle methods (`onCreate`, `onStartCommand`, `onDestroy`).
- Foreground notification registration/wiring details.
- Direct callback subscription wiring (`CallbackManager.subscribe(...)`).

## Migration Direction

- Move logic from `SteamService` companion/static helpers into domains first.
- Keep temporary companion facades only for compatibility; call-site migration should remove them over time.
- Keep manager classes as domain internals. New call sites should prefer domain APIs.

## Placement Rule of Thumb

- If the code answers "what should happen for Steam behavior?" -> domain.
- If the code answers "how does Android Service/wiring run?" -> `SteamService`.

