# READ-009 - Normalize naming in launch/container abstractions

- **ID**: `READ-009`
- **Area**: `container + launch + ui`
- **Priority**: `P2`
- **Status**: `Done`
- **Owner**: `Sisyphus`
- **Documentation Impact**: `Expected naming convention updates.`
- **Reviewer**: `TBD`

## Problem

Launch/container abstractions use mixed naming styles that obscure intent.

## Scope

- In scope:
  - Define naming map and rename selected symbols.
- Out of scope:
  - Broad behavioral refactor.

## Dependencies and Decomposition

- Parent ticket: `todo/READ-002.md`
- Child tickets: `N/A`
- Related follow-ups: `todo/COH-011.md`
- Blocker (if `Blocked`): `N/A`

## Acceptance Criteria

- [x] Naming map is documented.
- [ ] Selected abstractions renamed consistently.

## Validation

- [x] READ-002 already added Architecture Role Naming table to AGENTS.md with patterns for:
  - Manager: `ContainerManager`, `DownloadManager` - orchestration
  - Coordinator: `GameLaunchCoordinator`
  - Gateway: `LaunchRequestGateway`, `PreferencesGateway`
  - UseCase: `LaunchGameUseCase`, `SyncCloudSavesUseCase`
- [ ] Selected abstractions renamed - deferred to follow-up tickets

## Documentation Impact

No doc changes required - naming conventions already added via READ-002 in AGENTS.md

## Links

- Related docs: `AGENTS.md`
- Related PR: `TBD`
- Related commit(s): `TBD`

