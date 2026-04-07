# Architecture Decision Records (ADR)

This directory contains ADRs (Architecture Decision Records) that document key architectural decisions made during the refactoring process.

## What is an ADR?

An ADR is a document that captures an important architectural decision made along with its context and consequences.

## Format

Each ADR follows this structure:
- **Status**: Proposed, Accepted, Deprecated, or Superseded
- **Date**: Decision date
- **Context**: The situation that prompted the decision
- **Decision**: What we decided to do
- **Consequences**: What results from this decision (positive and negative)

## Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [ADR-001](ADR-001-ui-viewmodel-boundary.md) | UI-to-ViewModel Boundary Contract | Accepted | 2024-01-15 |
| [ADR-002](ADR-002-legacy-winlator-seam.md) | Legacy Winlator UI Seam as Controlled Exception | Accepted | 2024-01-15 |

## Related Documentation

- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Main architecture documentation
- [docs/ui-placement/ui-target-structure-map.md](../ui-placement/ui-target-structure-map.md) - UI placement rules
- [todo/INDEX.md](../../todo/INDEX.md) - All refactoring tickets
