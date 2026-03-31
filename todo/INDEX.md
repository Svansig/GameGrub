# Ticket Index

Use this file as the quick backlog board. Full details live in each ticket file.

## Refactor-Phase Execution Rule

Current execution is refactor-first. Prioritize tickets that reduce complexity and improve maintainability of existing behavior (`UI-*`, `COH-*`, `READ-*`, `SRV-*`, and refactor-supporting `TEST-*`/`DOC-*`).

During this phase, defer tickets that primarily expand scope beyond refactor goals unless they are required dependencies for active refactor work.

## Backlog - UI

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| UI-004 | P1 | Move library auth flow out of composables | `ui/screen/library` | `todo/UI-004.md` |
| UI-001 | P1 | Move GOG app operations out of UI class | `ui/screen/library/appscreen` | `todo/UI-001.md` |
| UI-002 | P1 | Remove unmanaged IO scopes from GOG UI paths | `ui/screen/library/appscreen` | `todo/UI-002.md` |
| UI-003 | P1 | Remove unmanaged IO scopes from Steam UI paths | `ui/screen/library/appscreen` | `todo/UI-003.md` |
| UI-005 | P1 | Decompose orchestration from `GameGrubMain` | `ui` | `todo/UI-005.md` |
| UI-006 | P2 | Resolve lint blockers in `GameGrubMain` | `ui/GameGrubMain.kt` | `todo/UI-006.md` |
| UI-007 | P2 | Align architecture doc path naming | `docs` | `todo/UI-007.md` |
| UI-008 | P3 | Move preview fake data to safer location | `ui/internal` | `todo/UI-008.md` |
| UI-009 | P3 | Define legacy UI seam guardrails | `com.winlator + res/layout` | `todo/UI-009.md` |
| UI-010 | P2 | Move platform login-state derivation into ViewModel state | `ui/screen/library` | `todo/UI-010.md` |
| UI-011 | P2 | Extract OAuth launcher callback wiring from screen composables | `ui/screen/library + settings` | `todo/UI-011.md` |
| UI-012 | P3 | Standardize dialog state ownership across screens | `ui/component/dialog + ui/screen` | `todo/UI-012.md` |
| UI-013 | P3 | Reduce direct event bus subscriptions inside composables | `ui/screen` | `todo/UI-013.md` |
| UI-014 | P2 | Extract route/navigation side effects out of screen-level composables | `ui/screen + ui/model` | `todo/UI-014.md` |
| UI-015 | P3 | Consolidate duplicate platform action components into shared UI primitives | `ui/component + ui/screen/library` | `todo/UI-015.md` |
| UI-016 | P2 | Replace global launch/network reads in UI with injected state gateways | `ui/GameGrubMain + ui/model` | `todo/UI-016.md` |
| UI-017 | P2 | Remove direct `PrefManager` access from target composables via ViewModel state | `ui/screen + ui/model` | `todo/UI-017.md` |

## Backlog - Cohesion

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| COH-001 | P1 | Define UI-to-ViewModel boundary contract | `ui/model` | `todo/COH-001.md` |
| COH-002 | P1 | Reduce service singleton usage from app layer | `service + ui` | `todo/COH-002.md` |
| COH-003 | P2 | Standardize event emission ownership | `events + service + ui` | `todo/COH-003.md` |
| COH-004 | P2 | Create module-level dependency map | `architecture` | `todo/COH-004.md` |
| COH-005 | P2 | Introduce use-case boundaries for launch and auth flows | `ui/model + service` | `todo/COH-005.md` |
| COH-006 | P2 | Standardize state/effect contracts across ViewModels | `ui/model` | `todo/COH-006.md` |
| COH-007 | P2 | Detect and reduce cyclic package dependencies | `app/gamegrub` | `todo/COH-007.md` |
| COH-008 | P2 | Add ADR notes for key architecture boundary decisions | `docs + architecture` | `todo/COH-008.md` |
| COH-009 | P2 | Define service-to-domain handoff checklist | `service` | `todo/COH-009.md` |
| COH-010 | P2 | Consolidate cross-platform auth flow ownership model | `service/auth + ui/model` | `todo/COH-010.md` |
| COH-011 | P3 | Standardize app-level coordinator placement rules | `app/gamegrub` | `todo/COH-011.md` |
| COH-012 | P3 | Create dependency guardrails for utils packages | `utils + architecture` | `todo/COH-012.md` |
| COH-013 | P2 | Define boundary between platform services and shared download pipeline | `service + downloader` | `todo/COH-013.md` |
| COH-014 | P3 | Introduce refactor checklist for cross-layer pull requests | `docs + process` | `todo/COH-014.md` |
| COH-015 | P2 | Introduce `LaunchRequestGateway` and migrate away from static launch request manager access | `app + ui/model` | `todo/COH-015.md` |
| COH-016 | P2 | Introduce `PreferencesGateway` and phase out direct global `PrefManager` reads | `app + service + ui` | `todo/COH-016.md` |

## Backlog - Readability

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| READ-001 | P1 | Split oversized Kotlin files by responsibility | `ui + service` | `todo/READ-001.md` |
| READ-002 | P2 | Normalize naming for manager/coordinator/domain classes | `service` | `todo/READ-002.md` |
| READ-003 | P2 | Replace ambiguous comments with intent-focused docs | `app/src/main/java` | `todo/READ-003.md` |
| READ-004 | P2 | Create package-level README notes for critical flows | `ui + service + utils` | `todo/READ-004.md` |
| READ-005 | P2 | Document public utility APIs with KDoc where needed | `utils` | `todo/READ-005.md` |
| READ-006 | P2 | Refactor long methods into testable helper units | `ui + service` | `todo/READ-006.md` |
| READ-007 | P3 | Standardize log tag/message conventions | `app/src/main/java` | `todo/READ-007.md` |
| READ-008 | P3 | Track and clean stale TODO comments with owners | `app/src/main/java` | `todo/READ-008.md` |
| READ-009 | P2 | Normalize naming in launch/container abstractions | `container + launch + ui` | `todo/READ-009.md` |
| READ-010 | P2 | Add focused package overviews for service domains | `service/steam/domain` | `todo/READ-010.md` |
| READ-011 | P3 | Standardize error-message phrasing style guide | `ui + service` | `todo/READ-011.md` |
| READ-012 | P3 | Add ownership headers for high-risk files | `ui + service + docs` | `todo/READ-012.md` |
| READ-013 | P2 | Standardize naming for async job and task identifiers | `service + background work` | `todo/READ-013.md` |
| READ-014 | P3 | Add concise file-level intent notes for critical legacy interop files | `com.winlator + integration points` | `todo/READ-014.md` |

## Backlog - Performance

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| PERF-001 | P1 | Add startup and launch baseline metrics | `app startup + launch` | `todo/PERF-001.md` |
| PERF-002 | P1 | Audit Compose recomposition hotspots | `ui` | `todo/PERF-002.md` |
| PERF-003 | P2 | Optimize download/install concurrency and backpressure | `service/steam + service/gog` | `todo/PERF-003.md` |
| PERF-004 | P2 | Profile Room queries and add index improvements | `db` | `todo/PERF-004.md` |
| PERF-005 | P2 | Optimize library list sort/filter performance at scale | `ui/screen/library + model` | `todo/PERF-005.md` |
| PERF-006 | P2 | Tune image loading and cache hit rates for library UI | `ui + image loading` | `todo/PERF-006.md` |
| PERF-007 | P3 | Reduce allocations in input and rendering hot paths | `xserver + input` | `todo/PERF-007.md` |
| PERF-008 | P3 | Add periodic performance regression tracking doc | `docs + profiling` | `todo/PERF-008.md` |
| PERF-009 | P2 | Add baseline profiling for auth and sync workflows | `service/auth + service/steam` | `todo/PERF-009.md` |
| PERF-010 | P2 | Optimize background work scheduling to reduce contention | `work scheduling + service` | `todo/PERF-010.md` |
| PERF-011 | P3 | Tune container launch prechecks for faster start | `container launch` | `todo/PERF-011.md` |
| PERF-012 | P3 | Add performance budget guardrails for key interactions | `ui + docs` | `todo/PERF-012.md` |
| PERF-013 | P2 | Reduce repeated parsing and allocation in auth/session hot paths | `service/auth + session` | `todo/PERF-013.md` |
| PERF-014 | P3 | Add lightweight memory-snapshot comparison workflow for refactor PRs | `profiling + docs` | `todo/PERF-014.md` |

## Backlog - Reliability

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| REL-001 | P1 | Standardize retry/backoff policy for network calls | `service + utils/network` | `todo/REL-001.md` |
| REL-002 | P1 | Harden cancellation and shutdown semantics | `service + ui` | `todo/REL-002.md` |
| REL-003 | P2 | Implement atomic file-write helpers for critical data | `utils/storage` | `todo/REL-003.md` |
| REL-004 | P2 | Add failure taxonomy and user-safe error mapping | `service + ui` | `todo/REL-004.md` |
| REL-005 | P2 | Define and enforce timeout policy per operation type | `service + network` | `todo/REL-005.md` |
| REL-006 | P2 | Add startup crash recovery guardrails | `app startup` | `todo/REL-006.md` |
| REL-007 | P2 | Improve foreground service restart resilience | `service` | `todo/REL-007.md` |
| REL-008 | P3 | Add migration rollback and recovery guidance | `db + docs` | `todo/REL-008.md` |
| REL-009 | P2 | Add idempotency checks for repeated operation requests | `service + downloads` | `todo/REL-009.md` |
| REL-010 | P2 | Harden offline-mode transitions across platforms | `ui + service` | `todo/REL-010.md` |
| REL-011 | P3 | Standardize recovery from partial install states | `service/install` | `todo/REL-011.md` |
| REL-012 | P3 | Add reliability incident review template | `docs + process` | `todo/REL-012.md` |
| REL-013 | P2 | Harden cache invalidation and stale-data fallback behavior | `service + data cache` | `todo/REL-013.md` |
| REL-014 | P3 | Add defensive guards for null/empty platform payload edge cases | `service/platform adapters` | `todo/REL-014.md` |

## Backlog - Testing

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| TEST-001 | P1 | Build a test gap matrix by feature | `tests` | `todo/TEST-001.md` |
| TEST-002 | P1 | Add auth and library regression tests | `ui/model + service` | `todo/TEST-002.md` |
| TEST-003 | P2 | Add download/install state machine tests | `service` | `todo/TEST-003.md` |
| TEST-004 | P2 | Add smoke tests for launch and resume flows | `ui + service + xserver` | `todo/TEST-004.md` |
| TEST-005 | P2 | Add contract tests for platform auth adapters | `service/auth` | `todo/TEST-005.md` |
| TEST-006 | P2 | Add Compose UI state tests for critical screens | `ui` | `todo/TEST-006.md` |
| TEST-007 | P3 | Add flaky test triage and quarantine workflow | `tests + CI` | `todo/TEST-007.md` |
| TEST-008 | P3 | Build shared test fixture builders for game/library data | `app/src/test` | `todo/TEST-008.md` |
| TEST-009 | P2 | Add unit tests for launch request queue behavior | `app + ui/model` | `todo/TEST-009.md` |
| TEST-010 | P2 | Add tests for cancellation and resume edge cases | `service + ui` | `todo/TEST-010.md` |
| TEST-011 | P3 | Add deterministic fake clock utilities for time-based tests | `app/src/test` | `todo/TEST-011.md` |
| TEST-012 | P3 | Add regression tests for settings persistence flows | `ui/screen/settings + PrefManager` | `todo/TEST-012.md` |
| TEST-013 | P2 | Add contract tests for service-domain boundary invariants | `service/domain tests` | `todo/TEST-013.md` |
| TEST-014 | P3 | Add snapshot tests for navigation state reducers | `ui/model + navigation` | `todo/TEST-014.md` |
| TEST-015 | P2 | Add contract tests for launch request gateway and pending-launch state behavior | `app/src/test` | `todo/TEST-015.md` |
| TEST-016 | P2 | Add regression tests for gateway-backed preferences and service facades | `app/src/test + service tests` | `todo/TEST-016.md` |

## Backlog - CI and Build

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| CI-001 | P1 | Add `lintKotlin` to PR checks | `.github/workflows` | `todo/CI-001.md` |
| CI-002 | P1 | Add test sharding and reporting improvements | `CI` | `todo/CI-002.md` |
| CI-003 | P2 | Add build cache and timing benchmarks | `Gradle + CI` | `todo/CI-003.md` |
| CI-004 | P2 | Add release signing guard checks | `build scripts` | `todo/CI-004.md` |
| CI-005 | P2 | Add nightly full matrix validation workflow | `.github/workflows` | `todo/CI-005.md` |
| CI-006 | P2 | Add static analysis gate for maintainability risks | `CI + static analysis` | `todo/CI-006.md` |
| CI-007 | P3 | Auto-link ticket IDs and PR metadata in CI checks | `CI automation` | `todo/CI-007.md` |
| CI-008 | P3 | Add PR risk labeling by changed areas | `.github/workflows` | `todo/CI-008.md` |
| CI-009 | P2 | Add changed-files based selective test execution | `.github/workflows + Gradle` | `todo/CI-009.md` |
| CI-010 | P2 | Add CI guard for required ticket lifecycle fields | `CI automation` | `todo/CI-010.md` |
| CI-011 | P3 | Publish CI runbook for common failure categories | `docs + CI` | `todo/CI-011.md` |
| CI-012 | P3 | Add artifact retention policy and cleanup automation | `CI + release artifacts` | `todo/CI-012.md` |
| CI-013 | P2 | Add gate ensuring changed refactor tickets include parent/child links when split | `CI automation + tickets` | `todo/CI-013.md` |
| CI-014 | P3 | Add CI summary comment for documentation impact compliance | `.github/workflows` | `todo/CI-014.md` |
| CI-015 | P3 | Add CI guard to flag new global singleton/companion access in touched files | `CI + static checks` | `todo/CI-015.md` |

## Backlog - Security

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| SEC-001 | P1 | Audit credential and token storage paths | `auth + storage` | `todo/SEC-001.md` |
| SEC-002 | P1 | Add dependency CVE review cadence | `dependencies` | `todo/SEC-002.md` |
| SEC-003 | P2 | Review external intent and deep-link validation | `MainActivity + launch` | `todo/SEC-003.md` |
| SEC-004 | P2 | Minimize sensitive data exposure in logs | `logging` | `todo/SEC-004.md` |
| SEC-005 | P2 | Add secret scanning workflow for commits and PRs | `CI + security` | `todo/SEC-005.md` |
| SEC-006 | P2 | Audit runtime permission usage and minimization | `Android manifest + runtime` | `todo/SEC-006.md` |
| SEC-007 | P3 | Review TLS and network security configuration hardening | `network + config` | `todo/SEC-007.md` |
| SEC-008 | P3 | Generate and publish SBOM for releases | `build + release` | `todo/SEC-008.md` |
| SEC-009 | P2 | Add threat-model summary for launch/auth surface | `docs + app + auth` | `todo/SEC-009.md` |
| SEC-010 | P2 | Add guardrails for external file input validation | `storage + imports` | `todo/SEC-010.md` |
| SEC-011 | P3 | Review exported components and manifest hardening | `AndroidManifest` | `todo/SEC-011.md` |
| SEC-012 | P3 | Add security review checklist for high-risk PRs | `docs + process` | `todo/SEC-012.md` |
| SEC-013 | P2 | Audit and reduce sensitive fields persisted in local debug artifacts | `storage + debug tooling` | `todo/SEC-013.md` |
| SEC-014 | P3 | Add hardening checklist for third-party SDK configuration | `build + runtime config` | `todo/SEC-014.md` |

## Backlog - Documentation

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| DOC-001 | P1 | Create architecture navigation index | `docs` | `todo/DOC-001.md` |
| DOC-002 | P2 | Add runbook for auth failures and recovery | `docs` | `todo/DOC-002.md` |
| DOC-003 | P2 | Add launch pipeline sequence diagram docs | `docs` | `todo/DOC-003.md` |
| DOC-004 | P2 | Add contribution examples for common refactors | `docs + contributing` | `todo/DOC-004.md` |
| DOC-005 | P2 | Add cross-project glossary for shared terminology | `docs` | `todo/DOC-005.md` |
| DOC-006 | P2 | Add contributor quickstart path by task type | `README + CONTRIBUTING + docs` | `todo/DOC-006.md` |
| DOC-007 | P3 | Build troubleshooting index with symptom mapping | `docs` | `todo/DOC-007.md` |
| DOC-008 | P3 | Add release readiness checklist and handoff notes | `docs + release process` | `todo/DOC-008.md` |
| DOC-009 | P2 | Add architecture decision timeline and change log | `docs + architecture` | `todo/DOC-009.md` |
| DOC-010 | P2 | Add glossary-linked onboarding map for new contributors | `README + docs` | `todo/DOC-010.md` |
| DOC-011 | P3 | Add platform-specific troubleshooting pages | `docs + platform flows` | `todo/DOC-011.md` |
| DOC-012 | P3 | Add docs maintenance cadence and ownership table | `docs + process` | `todo/DOC-012.md` |
| DOC-013 | P2 | Add refactor progress dashboard doc linked to ticket themes | `docs + todo` | `todo/DOC-013.md` |
| DOC-014 | P3 | Add codebase map for legacy-to-refactor transition zones | `docs + architecture` | `todo/DOC-014.md` |
| DOC-015 | P2 | Document global-state migration strategy and approved gateway patterns | `docs + architecture` | `todo/DOC-015.md` |

## Backlog - Service Refactoring

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| SRV-001 | P1 | Move download orchestration to SteamInstallDomain | `service/steam` | `todo/SRV-001.md` |
| SRV-002 | P2 | Move installer/download helpers to SteamInstallDomain | `service/steam` | `todo/SRV-002.md` |
| SRV-003 | P2 | Remove DepotDownloader import from SteamService | `service/steam` | `todo/SRV-003.md` |
| SRV-004 | P1 | Move session/launch orchestration to SteamSessionDomain | `service/steam` | `todo/SRV-004.md` |
| SRV-005 | P2 | Move login helper to SteamAccountDomain | `service/steam` | `todo/SRV-005.md` |
| SRV-006 | P2 | Move PICS/sync utility methods to appropriate domains | `service/steam` | `todo/SRV-006.md` |
| SRV-007 | P2 | Deprecate SteamService companion service locator pattern | `service/steam` | `todo/SRV-007.md` |
| SRV-008 | P3 | Remove library wrapper facades from companion | `service/steam` | `todo/SRV-008.md` |
| SRV-009 | P1 | Remove remaining class fields that belong in domains | `service/steam` | `todo/SRV-009.md` |
| SRV-010 | P2 | Move Steam Input config resolution to SteamInstallDomain | `service/steam` | `todo/SRV-010.md` |
| SRV-011 | P3 | Move Windows launch info helper to SteamInstallDomain | `service/steam` | `todo/SRV-011.md` |
| SRV-012 | P2 | Remove cloud stats wrapper facades from companion | `service/steam` | `todo/SRV-012.md` |
| SRV-013 | P2 | Consolidate shutdown/cleanup helpers | `service/steam` | `todo/SRV-013.md` |
| SRV-014 | P1 | Remove DAO injections from SteamService | `service/steam` | `todo/SRV-014.md` |
| SRV-015 | P3 | Consolidate companion constants into domain config | `service/steam` | `todo/SRV-015.md` |
| SRV-016a | P1 | Add download orchestration methods to SteamInstallDomain | `service/steam` | `todo/SRV-016a.md` |
| SRV-016b | P1 | Migrate download call sites to SteamInstallDomain | Done | `todo/SRV-016b.md` |
| SRV-016c | P2 | Clean up download-related imports and constants | Done | `todo/SRV-016c.md` |
| SRV-016 | P2 | Move authentication side effects from service callbacks into domains | `service/steam` | `todo/SRV-016.md` |
| SRV-017 | P2 | Replace remaining static facade entry points with injected pathways | `service/steam` | `todo/SRV-017.md` |
| SRV-018 | P2 | Split SteamService startup pipeline into explicit stages | `service/steam` | `todo/SRV-018.md` |
| SRV-019 | P3 | Introduce shared domain result wrappers for service operations | `service/steam/domain` | `todo/SRV-019.md` |
| SRV-020 | P3 | Add service-domain migration verification checklist | `service/steam + docs` | `todo/SRV-020.md` |
| SRV-021 | P2 | Move platform credential refresh flow behind domain boundary | `service/steam + auth domain` | `todo/SRV-021.md` |
| SRV-022 | P3 | Remove residual orchestration helpers from SteamService companion and route to domains | `service/steam` | `todo/SRV-022.md` |
| SRV-023 | P2 | Remove direct `SteamService.requireInstance/getInstance` usage from pre-launch utilities | `container launch + utils/preInstallSteps` | `todo/SRV-023.md` |
| SRV-024 | P2 | Replace `SteamService` companion data access wrappers with injected domain gateways | `service/steam + callers` | `todo/SRV-024.md` |
| SRV-025 | P3 | Reduce companion-object API surface in non-Steam platform services | `service/gog + service/epic + service/amazon` | `todo/SRV-025.md` |
| SRV-026 | P3 | Convert mutable global service flags to scoped state holders with explicit ownership | `service/steam + service/*` | `todo/SRV-026.md` |

## In Progress

| ID | Priority | Title | Owner | File |
|---|---|---|---|---|
| _none_ |  |  |  |  |

## Blocked

| ID | Priority | Title | Blocker | File |
|---|---|---|---|---|
| _none_ |  |  |  |  |

## Done

| ID | Priority | Title | PR | File |
|---|---|---|---|---|
| SRV-009 | P1 | Remove remaining class fields that belong in domains | TBD | `todo/SRV-009.md` |
| SRV-014 | P1 | Remove DAO injections from SteamService | TBD | `todo/SRV-014.md` |
| SRV-010 | P2 | Move Steam Input config resolution to SteamInstallDomain | TBD | `todo/SRV-010.md` |
| SRV-016a | P1 | Add download orchestration methods to SteamInstallDomain | TBD | `todo/SRV-016a.md` |
| SRV-016b | P1 | Migrate download call sites to SteamInstallDomain | TBD | `todo/SRV-016b.md` |
| SRV-016c | P2 | Clean up download-related imports and constants | TBD | `todo/SRV-016c.md` |
