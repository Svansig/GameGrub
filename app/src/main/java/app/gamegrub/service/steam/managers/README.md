# Steam Managers

This directory contains manager/helper components used by Steam domains.

## End Goal

Managers are focused, reusable implementation helpers. They should not become a second orchestration layer that bypasses domains.

In end-state architecture, managers:

- implement narrow algorithms or integration details,
- remain stateless or hold tightly scoped internal state,
- are composed by domains,
- are not called directly from UI or unrelated app layers.

## Role in the Stack

Target call flow:

`SteamService` (lifecycle + callbacks) -> `Steam*Domain` (workflow owner) -> `*Manager` (focused helper) -> DAO/client/filesystem

## Typical Manager Responsibilities

- Algorithmic helpers (depot selection, download planning, EXE scoring, DLC ownership checks).
- Protocol or file-format details (ticket/session files, controller config parsing/routing).
- Small integration adapters around specific Steam API operations.

## What Belongs Here

- Single-purpose logic with clear inputs/outputs.
- Reusable computations shared by one or more domain methods.
- Helper code that would otherwise bloat a domain method but does not define workflow policy.

## What Does Not Belong Here

- Full multi-domain business workflows.
- Android Service lifecycle behavior and callback wiring.
- Global app state management exposed through static companion APIs.

## Naming and Boundaries

- Keep names behavior-oriented (`SteamDepotSelectionManager`, `SteamInstallManager`, etc.).
- Prefer pure functions or small injected collaborators.
- Avoid direct use of `SteamService.instance` or companion-only state.
- If a manager starts coordinating multiple subsystems end-to-end, promote that logic into a domain.

## Migration Direction

- Existing manager-heavy logic currently in `SteamService` should move to domains first, then be split into managers where needed.
- New features should add domain entry points; managers stay internal implementation details.
- As call sites migrate, remove compatibility wrappers in `SteamService` companion.

## Placement Rule of Thumb

- If the code decides policy and sequence across multiple operations -> domain.
- If the code performs one operation well with clear inputs/outputs -> manager.

