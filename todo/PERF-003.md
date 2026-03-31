# PERF-003 - Optimize download/install concurrency and backpressure

- **ID**: `PERF-003`
- **Area**: `service/steam + service/gog`
- **Priority**: `P2`
- **Status**: `Backlog`
- **Owner**: `TBD`

## Problem

Concurrent download/install operations can create contention and uneven throughput.

## Scope

- In scope:
  - Review queueing/concurrency limits.
  - Add guardrails for CPU/network/disk pressure.
- Out of scope:
  - Platform API protocol redesign.

## Acceptance Criteria

- [ ] Current concurrency behavior documented.
- [ ] Tunable limits introduced or clarified.
- [ ] Throughput and stability comparison recorded.

## Validation

- [ ] Stress run with parallel downloads completes without regressions.

## Links

- Related docs: `ARCHITECTURE.md`
- PR: `TBD`

