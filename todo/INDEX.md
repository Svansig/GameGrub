# Ticket Index

Use this file as the quick backlog board. Full details live in each ticket file.

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

## Backlog - Cohesion

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| COH-001 | P1 | Define UI-to-ViewModel boundary contract | `ui/model` | `todo/COH-001.md` |
| COH-002 | P1 | Reduce service singleton usage from app layer | `service + ui` | `todo/COH-002.md` |
| COH-003 | P2 | Standardize event emission ownership | `events + service + ui` | `todo/COH-003.md` |
| COH-004 | P2 | Create module-level dependency map | `architecture` | `todo/COH-004.md` |

## Backlog - Readability

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| READ-001 | P1 | Split oversized Kotlin files by responsibility | `ui + service` | `todo/READ-001.md` |
| READ-002 | P2 | Normalize naming for manager/coordinator/domain classes | `service` | `todo/READ-002.md` |
| READ-003 | P2 | Replace ambiguous comments with intent-focused docs | `app/src/main/java` | `todo/READ-003.md` |
| READ-004 | P2 | Create package-level README notes for critical flows | `ui + service + utils` | `todo/READ-004.md` |

## Backlog - Performance

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| PERF-001 | P1 | Add startup and launch baseline metrics | `app startup + launch` | `todo/PERF-001.md` |
| PERF-002 | P1 | Audit Compose recomposition hotspots | `ui` | `todo/PERF-002.md` |
| PERF-003 | P2 | Optimize download/install concurrency and backpressure | `service/steam + service/gog` | `todo/PERF-003.md` |
| PERF-004 | P2 | Profile Room queries and add index improvements | `db` | `todo/PERF-004.md` |

## Backlog - Reliability

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| REL-001 | P1 | Standardize retry/backoff policy for network calls | `service + utils/network` | `todo/REL-001.md` |
| REL-002 | P1 | Harden cancellation and shutdown semantics | `service + ui` | `todo/REL-002.md` |
| REL-003 | P2 | Implement atomic file-write helpers for critical data | `utils/storage` | `todo/REL-003.md` |
| REL-004 | P2 | Add failure taxonomy and user-safe error mapping | `service + ui` | `todo/REL-004.md` |

## Backlog - Testing

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| TEST-001 | P1 | Build a test gap matrix by feature | `tests` | `todo/TEST-001.md` |
| TEST-002 | P1 | Add auth and library regression tests | `ui/model + service` | `todo/TEST-002.md` |
| TEST-003 | P2 | Add download/install state machine tests | `service` | `todo/TEST-003.md` |
| TEST-004 | P2 | Add smoke tests for launch and resume flows | `ui + service + xserver` | `todo/TEST-004.md` |

## Backlog - CI and Build

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| CI-001 | P1 | Add `lintKotlin` to PR checks | `.github/workflows` | `todo/CI-001.md` |
| CI-002 | P1 | Add test sharding and reporting improvements | `CI` | `todo/CI-002.md` |
| CI-003 | P2 | Add build cache and timing benchmarks | `Gradle + CI` | `todo/CI-003.md` |
| CI-004 | P2 | Add release signing guard checks | `build scripts` | `todo/CI-004.md` |

## Backlog - Security

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| SEC-001 | P1 | Audit credential and token storage paths | `auth + storage` | `todo/SEC-001.md` |
| SEC-002 | P1 | Add dependency CVE review cadence | `dependencies` | `todo/SEC-002.md` |
| SEC-003 | P2 | Review external intent and deep-link validation | `MainActivity + launch` | `todo/SEC-003.md` |
| SEC-004 | P2 | Minimize sensitive data exposure in logs | `logging` | `todo/SEC-004.md` |

## Backlog - Documentation

| ID | Priority | Title | Area | File |
|---|---|---|---|---|
| DOC-001 | P1 | Create architecture navigation index | `docs` | `todo/DOC-001.md` |
| DOC-002 | P2 | Add runbook for auth failures and recovery | `docs` | `todo/DOC-002.md` |
| DOC-003 | P2 | Add launch pipeline sequence diagram docs | `docs` | `todo/DOC-003.md` |
| DOC-004 | P2 | Add contribution examples for common refactors | `docs + contributing` | `todo/DOC-004.md` |

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
| _none_ |  |  |  |  |
