# Process Improvement Log

Use this running log to capture opportunities discovered while implementing or reviewing tickets.

## Entry Format

- **Date**: `YYYY-MM-DD`
- **Ticket**: `todo/<ID>.md`
- **PR/Commit**: `<link or sha>`
- **Type**: `Code Quality | Workflow Quality | Tooling | Documentation`
- **Opportunity**: `<what could be improved>`
- **Proposed Action**: `<small concrete change>`
- **Owner**: `<name or TBD>`
- **Status**: `Backlog | In Progress | Done`

## Entries

- **Date**: `2026-04-09`
- **Ticket**: `N/A (runtime exit contract + teardown reporting)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Teardown reliability work lacked a single contract and structured outcome signal, making clean-exit verification ad-hoc and log-driven.
- **Proposed Action**: Define a runtime exit contract, add phase-based teardown reporting, and capture per-component stop summaries to make pass/fail conditions explicit.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-09`
- **Ticket**: `N/A (Shovel Knight clean-exit classification)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: App-initiated teardown could report non-zero guest termination statuses as launch errors even when an intentional exit was already in progress, creating false-positive failure telemetry and noisy user-facing error logs.
- **Proposed Action**: Gate non-zero termination error reporting with exit-intent context so intentional shutdowns are logged informationally while preserving error reporting when no exit intent exists.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-09`
- **Ticket**: `N/A (teardown lifecycle hardening)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Runtime teardown paths could block indefinitely on connector/server joins and left shutdown thread lifecycle implicit.
- **Proposed Action**: Add bounded join timeouts and explicit accept/client thread tracking in runtime connectors/servers so session close has deterministic shutdown behavior and observable timeout warnings.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-09`
- **Ticket**: `N/A (Shovel Knight launch audio + exit-status investigation)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Guest env vars were using host-absolute Unix socket paths (for PulseAudio), which can break IPC when Wine runs in proot rootfs.
- **Proposed Action**: Standardize guest-facing env socket paths to guest namespace paths (for example `unix:/tmp/.sound/PS0`) and add launch-time socket readiness diagnostics.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-09`
- **Ticket**: `N/A (Shovel Knight launch audio + exit-status investigation)`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Status `137` signals from app-initiated teardown can be misread as primary launch failures in bug triage.
- **Proposed Action**: Add an exit-intent flag to termination classification and require triage templates to classify `137` as root-cause only with independent crash/kill evidence.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-09`
- **Ticket**: `N/A (attestation + compatibility API resilience)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Attestation key generation failures (`ProviderException` / keystore `-67`) retried every call, and compatibility API `429` responses were retried immediately.
- **Proposed Action**: Add attestation retry cooldown after provider/generic failures and add `Retry-After` aware in-memory API backoff for compatibility fetches.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `todo/ARCH-022b.md`, `todo/ARCH-022c.md`, `todo/ARCH-022d.md`, `todo/ARCH-022e.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `LibraryViewModel` mixed source data collection, presentation composition, and auth/refresh orchestration in one class.
- **Proposed Action**: Keep source aggregation behind `LibraryGateway.observeSourceSnapshot()`, move presentation composition to `BuildLibraryPresentationUseCase`, move refresh/OAuth completion to dedicated use cases, and extract reusable search/compatibility helpers.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `todo/ARCH-022.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Refactor tickets can stall when acceptance criteria and validation evidence are updated late.
- **Proposed Action**: Update each child ticket status/validation in the same commit that introduces its code changes and run targeted tests before closure.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `todo/ARCH-022a.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Domain filter policy rules were still colocated in `LibraryViewModel`, making ownership logic harder to reuse and independently test.
- **Proposed Action**: Keep library ownership/type/install bypass rules under `app.gamegrub.domain.library.policy` and keep ViewModel focused on orchestration.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `todo/ARCH-022.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Large refactors in one ticket create review and rollback risk when behavior and boundary changes land together.
- **Proposed Action**: Continue enforcing child-ticket slicing (`ARCH-022a`..`ARCH-022e`) with per-slice validation evidence before starting the next slice.
- **Owner**: `Copilot`
- **Status**: `In Progress`

- **Date**: `2026-04-06`
- **Ticket**: `N/A (store behavior alignment)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: A Steam-only startup cache introduced store-specific behavior drift versus other storefront paths.
- **Proposed Action**: Use shared marker-based install detection helpers for Steam in the library filter path and keep startup reconciliation focused on persisted-store flags.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `N/A (startup install-status reconciliation)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Non-Steam library rows preserved stale `is_installed` flags across restarts when no fresh store sync occurred, causing the Installed tab to miss real installs or show removed installs.
- **Proposed Action**: Run a one-shot startup reconciliation pass that checks marker files for GOG/Epic/Amazon and updates Room install flags before normal library rendering.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-06`
- **Ticket**: `todo/SRV-007.md`, `todo/SRV-024.md`
- **PR/Commit**: `8ecf7744`
- **Type**: `Code Quality`
- **Opportunity**: Domain classes that needed sentinel/capacity values were importing `SteamService` companion constants, inverting the dependency direction. The pattern of adding a `SteamDomainConstants.kt` file in the domain package cleanly breaks this cycle without changing values.
- **Proposed Action**: Adopt `SteamDomainConstants.kt` as the canonical source for all Steam domain-internal constants. As SRV-015 progresses, migrate the SteamService companion to delegate to these constants rather than declaring its own.
- **Owner**: `Copilot`
- **Status**: `In Progress`



- **Date**: `2026-04-06`
- **Ticket**: `todo/COH-030.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `utils/FormatUtils.kt` became unreachable dead code after the SteamFormatUtils migration but was never deleted, creating confusion about which formatter to use.
- **Proposed Action**: Confirmed zero callers via static grep, then deleted the file. Add a dead-code sweep step to the COH-02x ticket closure checklist.
- **Owner**: `Copilot`
- **Status**: `Done`



- **Date**: `2026-04-05`
- **Ticket**: `N/A (Steam package-to-app PICS bridge)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Package PICS sync updated package/license mappings but did not schedule app PICS for unresolved apps, leaving many rows without metadata/type and invisible to library filters.
- **Proposed Action**: After package sync, queue app PICS requests for app ids with missing app metadata (`receivedPICS == false`).
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-05`
- **Ticket**: `N/A (Steam owner-filter hardening)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Steam owner filtering could collapse the visible library to zero when `owner_account_id` metadata is missing or family/self ids are incomplete.
- **Proposed Action**: Resolve owner ids from self + family and treat missing item-owner metadata as unknown (fail-open) instead of dropping rows.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-05`
- **Ticket**: `N/A (Steam library visibility fix)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Steam tab empty-state logic was tied to auth state only, which hid cached Steam library entries after logout/offline transitions.
- **Proposed Action**: Gate Steam empty-state splash on both auth state and visible library data presence to preserve cached-library visibility.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-04`
- **Ticket**: `N/A (warning cleanup pass)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Repeated nullable-body patterns in HTTP call sites create noisy nullability warnings and hide real issues in compile output.
- **Proposed Action**: Consolidate network response handling around non-null body contracts after `isSuccessful` checks and run warning-focused cleanup in small batches.
- **Owner**: `Copilot`
- **Status**: `In Progress`

- **Date**: `2026-04-04`
- **Ticket**: `todo/UI-028.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: User-facing strings in main/settings/feedback flows drifted into Kotlin files instead of shared resources, which fragments localization ownership.
- **Proposed Action**: Keep UI copy resource-backed by default and gate new hardcoded user text in `ui/*` with periodic magic-literal audits.
- **Owner**: `Copilot`
- **Status**: `In Progress`

- **Date**: `2026-04-04`
- **Ticket**: `todo/UI-028.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Local validation stalls when Java/Gradle prerequisites are missing, delaying ticket closure evidence.
- **Proposed Action**: Add a lightweight preflight check step (`JAVA_HOME` + `java -version`) before starting validation commands for Android tickets.
- **Owner**: `Copilot`
- **Status**: `Backlog`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005a.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `GameGrubMain` contained a long pre-launch orchestration pipeline that mixed non-UI launch logic with composable orchestration.
- **Proposed Action**: Keep launch orchestration in `ui/launch` helpers and let `GameGrubMain` act as a caller/orchestrator shell.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005a.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Launch orchestration previously created unmanaged IO scopes, making cancellation and ownership less predictable.
- **Proposed Action**: Require caller-owned lifecycle scope injection for launch orchestration entry points (`preLaunchApp(scope = ...)`).
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005b.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: External-launch success handling and not-installed dialog mapping were duplicated across multiple `GameGrubMain` launch branches, increasing behavior-drift risk during refactors.
- **Proposed Action**: Keep launch-branch side effects centralized in file-private helpers until a dedicated launch coordinator/use-case is introduced.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005b.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Small `GameGrubMain` cleanups can be delayed because they are not split into child tickets with explicit behavior-parity scope.
- **Proposed Action**: Decompose large shell tickets into behavior-preserving child slices first, then run targeted validation per slice before broader coordinator extraction.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005c.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Launch resolution and Steam login gate logic lived in a UI launch orchestrator file, increasing cross-layer coupling.
- **Proposed Action**: Keep resolution and login-gate policies in launch-domain helpers (`app.gamegrub.launch`) and keep UI launch orchestrator focused on launch setup flow.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005d.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Launch telemetry emission was colocated with orchestration code, making ownership and testing seams less clear.
- **Proposed Action**: Isolate launch telemetry in launch-owned helpers and consume from orchestration/UI call sites.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005e.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: UI messaging helpers (snackbar/dialog construction) and launch prep logic were mixed in one file.
- **Proposed Action**: Keep UI message mapping in dedicated UI launch helper files and leave launch orchestrator focused on pre-launch pipeline steps.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-03`
- **Ticket**: `todo/UI-005.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Remaining `GameGrubMain` ownership gaps were broad enough to risk partial fixes and unclear review scope.
- **Proposed Action**: Split remaining gaps into targeted child tickets (`UI-005f`..`UI-005k`) with one responsibility boundary per ticket and parity-focused validation.
- **Owner**: `Sisyphus`
- **Status**: `In Progress`

- **Date**: `2026-04-01`
- **Ticket**: `todo/COH-023.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Network ownership was split between `NetworkMonitor` and `utils/network/Net`, which increased drift risk for connectivity policy and HTTP defaults.
- **Proposed Action**: Keep connectivity state and transport infrastructure in `app.gamegrub.network.NetworkManager` and enforce compatibility wrappers as delegation-only.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `todo/COH-023.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Large cross-package infra migrations are error-prone without a quick ownership audit step.
- **Proposed Action**: Add a lightweight checklist for infra migrations: static search for old surface usage, wrapper-only compatibility validation, and docs boundary update before marking tickets done.
- **Owner**: `TBD`
- **Status**: `Backlog`

- **Date**: `2026-04-01`
- **Ticket**: `todo/COH-022.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Storage helpers were fragmented across multiple utility objects, causing duplicated file/marker logic in service and UI layers.
- **Proposed Action**: Keep storage operations in a dedicated storage package and require caller-layer delegation through storage-owned APIs.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `todo/COH-018.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Device and hardware reads were duplicated across service, UI, and utility layers, increasing drift and version-specific behavior risk.
- **Proposed Action**: Keep all hardware/device queries behind `DeviceQueryGateway` and prohibit new direct reads outside the gateway implementation.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `N/A (immersive ownership hardening)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Legacy utility code retained duplicate immersive-mode writes outside the manager-owned path.
- **Proposed Action**: Keep a single immersive owner (`ImmersiveModeManager`) and restrict legacy utilities to delegation-only shims.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `todo/REL-015.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Orientation updates were emitted from composable body execution, creating avoidable recomposition-driven event churn.
- **Proposed Action**: Require orientation/system side effects to be emitted from keyed effect blocks (`LaunchedEffect`/lifecycle coordinators) and add lint guidance for event emissions in composable bodies.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-01`
- **Ticket**: `todo/TEST-017.md`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Local verification for orientation tickets can be blocked when `JAVA_HOME` is missing, delaying regression validation.
- **Proposed Action**: Add a short contributor runbook note for Java setup validation before Gradle tasks (`java -version`, `JAVA_HOME`) and include it in troubleshooting docs.
- **Owner**: `TBD`
- **Status**: `Backlog`

- **Date**: `2026-04-01`
- **Ticket**: `N/A (Steam PICS sync stabilization)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Long-lived Steam sync jobs were started without stored Job references, making cancellation ineffective and allowing duplicate background loops after reconnects.
- **Proposed Action**: Require every continuous domain worker to store/cancel its active `Job` in one place and add a reconnect regression test for duplicate workers.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (compile stabilization pass)`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Compile-only regressions were introduced during service/domain refactors without a fast guard for cross-file API moves.
- **Proposed Action**: Add a lightweight CI/local gate that always runs `:app:compileDebugKotlin` after Steam refactor slices.
- **Owner**: `TBD`
- **Status**: `Backlog`

- **Date**: `2026-03-31`
- **Ticket**: `todo/UI-004.md`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: OAuth callback parsing logic was duplicated across composables and difficult to unit test.
- **Proposed Action**: Keep auth-result parsing in small pure helpers with dedicated tests before wiring side effects in ViewModel.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (local dedup cleanup)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `BestConfigService` repeated key-presence checks and driver-version parsing logic in multiple branches.
- **Proposed Action**: Centralize JSON field extraction and parsing in private helpers to cut copy/paste paths and reduce drift risk.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (Hilt compile fix)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: `@HiltViewModel` classes were accessed from singleton entry points, which bypasses ViewModel lifecycle ownership and breaks Hilt validation.
- **Proposed Action**: Standardize app-screen wiring to resolve `@HiltViewModel` only through Compose/Android ViewModel APIs and pass instances into non-composable screen models.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-03-31`
- **Ticket**: `N/A (compile warning cleanup pass)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: Nullability drift left several impossible null checks and one unchecked cast, obscuring intent and increasing warning noise during `:app:compileDebugKotlin`.
- **Proposed Action**: Prefer non-null return contracts in call sites, use safe collection restoration in savers, and update deprecated Material3/WebView usage during warning cleanup slices.
- **Owner**: `TBD`
- **Status**: `Done`

- **Date**: `2026-04-11`
- **Ticket**: `N/A (JNI runtime measurement pass)`
- **PR/Commit**: `TBD`
- **Type**: `Workflow Quality`
- **Opportunity**: Stop-time-only performance counters were difficult to collect in scripted device runs because the session exit UI path was not reliably automatable from adb.
- **Proposed Action**: Keep a debug-only on-demand stats dump path available for future runtime investigations so measurement is not blocked on teardown automation.
- **Owner**: `Copilot`
- **Status**: `Done`

- **Date**: `2026-04-11`
- **Ticket**: `N/A (JNI runtime measurement pass)`
- **PR/Commit**: `TBD`
- **Type**: `Code Quality`
- **Opportunity**: External launch could navigate to `XServerScreen` with an empty `launchedAppId`, turning performance measurement into a crash/debugging detour.
- **Proposed Action**: Reassert launch-critical state at the final launch boundary and add a targeted regression check for external-launch-to-XServer navigation.
- **Owner**: `Copilot`
- **Status**: `Done`

