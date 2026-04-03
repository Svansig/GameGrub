# ARCH-027 - Remove Static Service Locators

- **ID**: `ARCH-027`
- **Area**: `service`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `No doc changes required` - Internal refactor
- **Reviewer**: `TBD`

## Problem

Services still use static companion object access patterns (getInstance()) instead of dependency injection.

## Scope

- In scope:
  - Identify all static service access patterns
  - Replace with injected gateways where possible
  - Keep for Android service lifecycle only
- Out of scope:
  - UI changes

## Acceptance Criteria

- [ ] No new static service access added
- [ ] Static access reduced by 50%

## Links

- Related docs: `N/A`
- Related PR: `TBD`
- Related commit(s): `TBD`
