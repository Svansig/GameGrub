# ARCH-012 - Consolidate Event Bus Usage

- **ID**: `ARCH-012`
- **Area**: `events + ui`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

GameGrubApp.events (event bus) is used extensively across UI and services. Need to consolidate to use gateway/state patterns instead.

## Scope

- In scope:
  - Identify all event bus usages
  - Replace events with state/gateway calls where possible
  - Keep events for truly global notifications only
- Out of scope:
  - Android-specific events

## Dependencies and Decomposition

- Parent ticket: `N/A`
- Child tickets: `N/A`
- Related follow-ups: `N/A`
- Blocker (if `Blocked`): `ARCH-011`

## Acceptance Criteria

- [ ] Event bus usages reduced by 50%
- [ ] Clear event vs state distinction

## Validation

- [ ] Tests pass
- [ ] `./gradlew lintKotlin` passes
- [ ] PR description includes `Documentation Impact`.

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
