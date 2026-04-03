# ADR-004 - Unified Game Store Architecture

## Status
Accepted

## Context
GameNative started as Steam-focused and had GOG, Epic, and Amazon added as separate implementations, each with:
- Separate service classes extending Android Service
- Separate DAOs and entities
- Duplicate patterns for auth, downloads, cloud saves
- No unified way to query all games

## Decision
Create unified abstractions:

1. **Unified Game Model**: `UnifiedGame` with `GameSource` discriminator
2. **Service Base Class**: `GameStoreService` for common service lifecycle
3. **Gateway Pattern**: `LibraryGateway`, `AuthGateway`, `LaunchGateway`, etc.
4. **Interface Abstractions**: `GameStoreAuth`, `GameStoreDownloader`, `GameStoreCloudSaves`

## Consequences

### Positive
- Single source of truth for library data
- Reduced code duplication (~500+ lines removed so far)
- Consistent API across stores
- Easier to add new stores

### Negative
- Migration effort required
- Some performance overhead from abstraction

## Related Tickets
- ARCH-001: Unified Game Domain Model
- ARCH-002: Unified Service Abstraction
- ARCH-003: Unified Launch Strategy
- ARCH-004: Unified Authentication
- ARCH-005: Unified Download Management
- ARCH-007: Cloud Saves Unification
