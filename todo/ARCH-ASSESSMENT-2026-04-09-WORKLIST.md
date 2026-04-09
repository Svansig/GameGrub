# Architecture Assessment Work List (2026-04-09)

Source: `docs/architecture-assessment-2026-04-09.md`

This work list turns the assessment roadmap into an execution order that follows the current refactor-first rule.

## Execution Rules

- Reduce launch/runtime risk first, then boundary cleanup, then scale/performance.
- Prefer existing tickets; only create child tickets when a step cannot be completed safely in one pass.
- Keep ticket lifecycle current during execution (`Backlog` -> `In Progress` -> `Done` or `Blocked`).

## Horizon 1 (0-30 days): Stabilize Reliability and Release Safety

### Track A - Runtime migration closure (highest priority)

- [ ] `ARCH-046` -> SessionAssembler implementation closure
- [ ] `ARCH-053-FOLLOWUP` -> orchestrator wiring to SessionAssembler/LaunchEngine
- [ ] `ARCH-055-FOLLOWUP` -> launch telemetry wiring via LaunchEngine
- [ ] `ARCH-067` -> split-root container support
- [ ] `ARCH-069` -> replace remaining mutable runtime mutation paths

Dependencies:
- Keep order as listed; each step reduces migration overlap for the next.

### Track B - Security/policy hardening decisions

- [ ] `SEC-003` -> external intent/deep-link validation
- [ ] `SEC-006` -> permission usage minimization
- [ ] `SEC-011` -> exported component hardening
- [ ] `SEC-016` -> gate analytics startup by privacy consent

Dependencies:
- `SEC-006` and `SEC-011` should inform manifest decision updates.

### Track C - P0 test safety net

- [ ] `TEST-001` -> test gap matrix by feature
- [ ] `TEST-002` -> auth/library regressions
- [ ] `TEST-004` -> launch/resume smoke tests
- [ ] `TEST-013` -> service-domain boundary contract tests

Dependencies:
- Use `TEST-001` as planning input for `TEST-002`, `TEST-004`, `TEST-013`.

### Track D - CI/release gate verification

- [ ] Verify `CI-001` (`lintKotlin` in PR checks) remains enforced; reopen if drift exists.
- [ ] Verify `CI-004` (release signing guard checks) remains enforced; reopen if drift exists.

## Horizon 2 (31-90 days): Complete Boundary Cleanup

### Track A - Steam service decomposition completion

- [ ] `SRV-001`
- [ ] `SRV-004`
- [ ] `SRV-014`
- [ ] `SRV-017`
- [ ] `SRV-018`
- [ ] `SRV-024`

Dependencies:
- Finish `SRV-014` before broad call-site migration work in `SRV-017`/`SRV-024`.

### Track B - UI ownership and gateway migration

- [ ] `UI-001`
- [ ] `UI-002`
- [ ] `UI-003`
- [ ] `UI-017`
- [ ] `COH-015`
- [ ] `COH-016`

Dependencies:
- Use `COH-015`/`COH-016` interfaces to eliminate direct global reads in UI tickets.

### Track C - Utils ownership migration

- [ ] `COH-024`
- [ ] `COH-025`
- [ ] `COH-026`
- [ ] `COH-027`
- [ ] `COH-028`
- [ ] `COH-029`
- [ ] `COH-030`

Dependencies:
- Execute in numeric order to preserve parent/child migration intent.

### Track D - Reliability hardening

- [ ] `REL-005`
- [ ] `REL-007`
- [ ] `REL-010`
- [ ] `REL-013`
- [ ] `REL-016`
- [ ] `REL-018`

## Horizon 3 (90-180 days): Scale Architecture and Performance

### Track A - Modularization foundation

- [ ] `COH-007` -> cyclic dependency reduction baseline
- [ ] `ARCH-016` -> gateway implementations
- [ ] `ARCH-017` -> Hilt modules for abstractions
- [ ] `ARCH-020` -> gateway DI bindings

### Track B - Performance baseline and budgets

- [ ] `PERF-001`
- [ ] `PERF-002`
- [ ] `PERF-004`
- [ ] `PERF-005`
- [ ] `PERF-012`

Dependencies:
- `PERF-001` first; use its baseline to validate impact of later perf tickets.

### Track C - Security hardening program

- [ ] `SEC-001`
- [ ] `SEC-004`
- [ ] `SEC-007`
- [ ] `SEC-009`
- [ ] `SEC-013`

### Track D - Documentation and contributor scaling

- [ ] `DOC-009`
- [ ] `DOC-013`
- [ ] `DOC-015`

## Horizon 4 (180+ days): Best-in-Class Product Architecture

### Track A - Store unification program

- [ ] `ARCH-001` through `ARCH-020` via existing child tickets first (`ARCH-001a`..`ARCH-007d`)

### Track B - CI intelligence and architecture guardrails

- [ ] `CI-006`
- [ ] `CI-009`
- [ ] `CI-010`
- [ ] `CI-014`
- [ ] `CI-015`

### Track C - Operating model

- [ ] Define and review SLOs quarterly (launch success, time-to-play, install success, crash-free sessions)
- [ ] Re-run architecture assessment monthly and rebalance roadmap quarterly

## Review Cadence

- Weekly: horizon progress check and blocker triage
- Monthly: architecture scorecard refresh against `docs/architecture-assessment-2026-04-09.md`
- Quarterly: roadmap reprioritization based on telemetry + delivery outcomes

