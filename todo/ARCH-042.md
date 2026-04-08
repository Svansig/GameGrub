# ARCH-042 - Design CacheController key derivation and invalidation policy

- **ID**: `ARCH-042`
- **Area**: `cache + storage`
- **Priority**: `P1`
- **Status**: `Backlog`
- **Owner**: `TBD`
- **Documentation Impact**: `Creates docs/cache-controller-design.md with key derivation, invalidation policies, and GC strategy.`
- **Reviewer**: `TBD`

## Problem

Caches (shader, translated code, probe) must be keyed by exact runtime/profile/game identity and managed with clear invalidation policies. Without deterministic keying, cache reuse is unsafe.

## Scope

- In scope:
  - Define cache key derivation:
    - For graphics/shader caches: SHA256(base-id + runtime-id + driver-id + game-exe-hash)
    - For translator caches: SHA256(runtime-id + game-exe-hash)
    - For probe caches: SHA256(base-id + game-exe-hash)
  - Document invalidation policies:
    - NEVER_INVALIDATE (caches are always valid once created)
    - INVALIDATE_ON_RUNTIME_CHANGE (clear when runtime version changes)
    - INVALIDATE_ON_DATE (clear after N days)
    - INVALIDATE_MANUALLY (user can explicitly clear)
  - Design garbage collection strategy:
    - Delete caches not used in > N days
    - Delete caches for uninstalled games
    - Reserve quota for system caches
  - Create `CacheKeyDeriver` interface and implementation
  - Document safe cache attachment/detachment
- Out of scope:
  - Implementing the cache controller (ARCH-043)
  - Graphics component integration (Phase 6)
  - GC execution (deferred)

## Dependencies and Decomposition

- Parent ticket: `ARCH-041`
- Child tickets: `ARCH-043`
- Related follow-ups: `ARCH-044` (cache wiring in session assembly)
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [ ] Documentation created at `docs/cache-controller-design.md` with:
  - [ ] Cache key derivation algorithm and examples
  - [ ] Invalidation policies and decision tree
  - [ ] Garbage collection policy and threshold
  - [ ] Storage quota management rules
- [ ] `CacheKeyDeriver` interface designed and partially implemented
- [ ] Cache key derivation is deterministic and stable
- [ ] Unit tests for key derivation:
  - [ ] Same inputs → same key
  - [ ] Different runtime/driver/game → different key
  - [ ] Key format is valid directory name

## Validation

- [ ] Second reviewer validates cache key derivation algorithm
- [ ] GC policy is reasonable for mobile storage constraints
- [ ] Documentation is clear and comprehensive
- [ ] PR description includes `Documentation Impact`.
- [ ] Implementation commit created before review.
- [ ] Independent review completed and recorded.
- [ ] Post-review changes committed.
- [ ] Improvement opportunities logged in `docs/process-improvement-log.md`.

## Links

- Related docs: `docs/cache-controller-design.md` (to be created)
- Related PR: `TBD`
- Related commit(s): `TBD`

