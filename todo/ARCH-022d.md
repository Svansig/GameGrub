# ARCH-022d - Extract Refresh and OAuth Orchestration

- **ID**: `ARCH-022d`
- **Area**: `ui/model`, `domain/usecase`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - orchestration refactor
- **Reviewer**: `TBD`

## Problem

`LibraryViewModel` directly coordinates refresh and store-specific OAuth completion flows.

## Scope

- In scope:
  - Add unified refresh orchestration use case (gateway-driven)
  - Add unified OAuth callback coordinator keyed by `GameSource`
  - Reduce direct service calls in ViewModel
- Out of scope:
  - UI redesign of auth messaging

## Dependencies and Decomposition

- Parent ticket: `ARCH-022`
- Child tickets: `N/A`
- Related follow-ups: `ARCH-022c`
- Blocker (if `Blocked`): `ARCH-022c`

## Acceptance Criteria

- [ ] ViewModel delegates refresh and OAuth orchestration to use-case/coordinator layer
- [ ] Auth state + sync behavior remains equivalent

## Validation

- [ ] Unit tests for orchestration success/failure and message mapping paths

## Links

- Related docs: `docs/adr/ADR-004-unified-game-store-architecture.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

