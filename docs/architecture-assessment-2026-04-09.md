# Architecture Assessment - 2026-04-09

> Scope: full-stack application architecture assessment (Android app, launch/runtime, service/domain boundaries, quality gates, and delivery readiness).
>
> Method: evidence-first review of current project docs and configuration files, with roadmap alignment to existing tickets in `todo/INDEX.md`.

## Executive Assessment

GameGrub has made strong progress in architecture hardening (especially launch/runtime foundations), but it is still in a high-risk transition period where old and new architecture models coexist.

The codebase is moving in the right direction, yet the current bottlenecks are now concentrated in boundary consistency, test coverage at critical seams, and release/CI rigor.

### Current Maturity Snapshot (0-5)

| Category | Score | Confidence | Notes |
|---|---:|---|---|
| Runtime launch architecture | 3.5 | High | Phase 0-11 migration complete in docs, but key integration follow-ups remain reopened (`ARCH-046`, `ARCH-053-FOLLOWUP`, `ARCH-055-FOLLOWUP`, `ARCH-067`, `ARCH-069`). |
| Service/domain boundaries | 2.5 | Medium | Steam decomposition advanced but still partial; `SteamService` remains a large coordinator/business-logic hybrid in latest decomposition snapshot. |
| UI/ViewModel boundary health | 3.0 | Medium | Major `UI-005*` decomposition work landed, but additional UI/state ownership cleanup tickets remain. |
| Data/storage boundaries | 3.0 | High | Storage ownership contract established and migration progressed; residual migration tasks still present in backlog and runtime mutation audit. |
| Modularity/build architecture | 2.0 | High | Project still primarily single app module (`:app`) plus dynamic feature (`:ubuntufs`), limiting compile isolation and ownership boundaries. |
| Testing architecture | 2.0 | Medium | Broad test backlog (`TEST-*`) indicates known regression-risk areas are not fully protected yet. |
| Security/policy posture | 2.5 | High | Manifest audit identifies policy-sensitive permissions and cleartext traffic decisions still open. |
| Release/CI architecture | 2.0 | Medium | `release` build type still debug-signed in `app/build.gradle.kts`; project guidance notes missing lint in PR checks. |

## What Is Going Well

- Runtime architecture redesign is well-structured and documented (`docs/runtime-architecture-overview.md`) with clear phase decomposition.
- Ownership-contract pattern is improving boundary clarity (orientation, immersive mode, device query, storage) in `ARCHITECTURE.md`.
- Refactor governance is strong: ticket decomposition and explicit follow-up tracking in `todo/INDEX.md`.
- Utility ownership audit is concrete and actionable (`docs/utils-ownership-audit-2026-04-03.md`), with migration tickets already created.

## Highest-Risk Gaps (Ordered)

1. **Transition architecture overlap is still active in launch/runtime paths**
   - Evidence: reopened migration tickets and mutation points still marked "To migrate" (`docs/mutation-points-audit.md`, `todo/INDEX.md`).
   - Product risk: launch reliability regressions and hard-to-debug edge cases under real-world variance.

2. **Service/domain decomposition is incomplete in the largest complexity hotspot**
   - Evidence: `docs/steam-service-decomposition-plan.md` shows partial completion and residual manager/static coupling.
   - Product risk: slow iteration speed, fragile refactors, and broad blast radius for launch/auth/download changes.

3. **Insufficient test guardrails for critical user journeys**
   - Evidence: large open `TEST-*` backlog and architecture notes on coverage gaps.
   - Product risk: regressions in P0 flows (launch, install, auth, error recovery) reaching users.

4. **Release engineering and compliance posture has unresolved risk**
   - Evidence: `release` build type signs with debug config (`app/build.gradle.kts`); manifest audit flags policy-sensitive permissions and cleartext traffic.
   - Product risk: release hygiene issues, policy friction, and avoidable trust/safety concerns.

5. **Limited module boundaries constrain long-term architecture scaling**
   - Evidence: `settings.gradle.kts` includes only `:app` and `:ubuntufs`.
   - Product risk: build performance drag, weak ownership enforcement, and increased merge conflict surface.

## Path To The Best Possible Product

The best possible product for GameGrub is:
- highly reliable launch/install/auth flows,
- predictable performance on constrained devices,
- policy-safe and security-hardened distribution,
- fast contributor velocity with low regression risk.

### Target State (North Star)

- **Architecture**: clear boundaries (UI -> use cases/gateways -> domain/data), no static singleton business dependencies in feature code.
- **Launch runtime**: SessionPlan-driven end-to-end execution with no shared mutable ImageFs path left in production callers.
- **Quality**: all P0/P1 user flows protected by regression tests and CI quality gates.
- **Delivery**: deterministic release pipeline with signing, lint, tests, and policy checks as enforced gates.
- **Scalability**: phased modularization for core contracts (`core:model`, `core:domain`, `core:data`, `core:ui`) and feature isolation.

## Prioritized Roadmap

### Horizon 1 (0-30 days): Stabilize Core Reliability and Release Safety

- Close runtime migration blockers first: `ARCH-046`, `ARCH-053-FOLLOWUP`, `ARCH-055-FOLLOWUP`, `ARCH-067`, `ARCH-069`.
- Land CI/release safety essentials: `CI-001` and `CI-004`.
- Resolve immediate policy decisions and mitigation plan for manifest risks: `SEC-003`, `SEC-006`, `SEC-011`, `SEC-016`.
- Build P0 test safety net for launch/auth/install/failure flows: `TEST-001`, `TEST-002`, `TEST-004`, `TEST-013`.

### Horizon 2 (31-90 days): Complete Boundary Cleanup in High-Churn Areas

- Finish service/domain decomposition path for Steam and remove static manager leak paths (`SRV-001`, `SRV-004`, `SRV-014`, `SRV-017`, `SRV-018`, `SRV-024`).
- Continue UI ownership extraction and gateway migrations (`UI-001`, `UI-002`, `UI-003`, `UI-017`, `COH-015`, `COH-016`).
- Execute utils ownership migration slices (`COH-024` through `COH-030`) to reduce architecture ambiguity.
- Add reliability policies and resilience hardening (`REL-005`, `REL-007`, `REL-010`, `REL-013`, `REL-016`, `REL-018`).

### Horizon 3 (90-180 days): Product-Scale Architecture and Performance

- Begin multi-module extraction with minimal-risk contracts first (`core:model`, `core:domain`), then `core:data` and feature slices.
- Run performance baseline and regression loop (`PERF-001`, `PERF-002`, `PERF-004`, `PERF-005`, `PERF-012`).
- Complete security hardening program (`SEC-001`, `SEC-004`, `SEC-007`, `SEC-009`, `SEC-013`).
- Improve documentation navigation and contributor ramp-up (`DOC-009`, `DOC-013`, `DOC-015`).

### Horizon 4 (180+ days): Best-in-Class Product Architecture

- Full store-domain unification and shared abstractions (`ARCH-001` through `ARCH-020` track) with compatibility preserved.
- Mature CI intelligence (selective tests, static architecture checks, policy guardrails): `CI-006`, `CI-009`, `CI-010`, `CI-014`, `CI-015`.
- Continuous quality model: SLOs for launch success, startup time, install completion, and crash-free sessions.

## Product-Level Success Metrics (Recommended)

- **Launch success rate**: p50/p95 success trend by store and device class.
- **Median time-to-play**: tap Play -> game process ready.
- **Install success rate** and recovery success after interruption.
- **Crash-free sessions** and foreground-service failure/restart rate.
- **Regression escape rate**: post-release defects per release affecting P0/P1 stories.
- **Refactor throughput**: ticket cycle time with reopened-ticket ratio.

## Recommended Governance Cadence

- Monthly architecture review against this scorecard.
- Quarterly roadmap rebalance using production telemetry and ticket burn-down data.
- Every architecture PR: explicit boundary statement + test impact + docs impact.

## Evidence Reference

- `ARCHITECTURE.md`
- `docs/runtime-architecture-overview.md`
- `docs/mutation-points-audit.md`
- `docs/steam-service-decomposition-plan.md`
- `docs/utils-ownership-audit-2026-04-03.md`
- `docs/android-manifest-audit.md`
- `todo/INDEX.md`
- `settings.gradle.kts`
- `app/build.gradle.kts`

