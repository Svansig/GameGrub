# ARCH-028 - Consolidate Event Bus Usage

- **ID**: `ARCH-028`
- **Area**: `events`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Event bus (EventDispatcher) is used inconsistently across the codebase. Some events go through bus, others bypass it.

## Scope

- In scope:
  - Audit event bus usage
  - Create unified event types
  - Deprecate direct event firing where appropriate
- Out of scope:
  - UI layer changes

## Acceptance Criteria

- [ ] All game-related events go through GameStoreEvent
- [ ] Event bus usage documented

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
