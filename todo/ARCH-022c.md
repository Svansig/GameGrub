# ARCH-022c - Replace Direct DAO Collection With LibraryGateway

- **ID**: `ARCH-022c`
- **Area**: `ui/model`, `gateway`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - boundary refactor
- **Reviewer**: `TBD`

## Problem

`LibraryViewModel` collects store-specific DAO flows directly, violating unified data boundary goals.

## Scope

- In scope:
  - Consume aggregated library streams via `LibraryGateway`
  - Remove direct DAO collector wiring from `LibraryViewModel`
- Out of scope:
  - Full rewrite of filter logic

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022b`
- Blocker (if `Blocked`): `ARCH-022b`

## Acceptance Criteria

- [ ] ViewModel no longer directly subscribes to per-store DAO streams
- [ ] Equivalent merged library state still renders correctly

## Validation

- [ ] Unit/integration tests covering gateway-fed library updates

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

