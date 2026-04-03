# Utils Ownership Audit (2026-04-03)

This audit covers every Kotlin file under `app/src/main/java/app/gamegrub/utils`.

Decision rule used:
- Keep in utils only if code is generic, side-effect-light, and not owned by a specific platform/domain workflow.
- Move when code is workflow ownership (auth, launch, install, service orchestration, platform state, policy enforcement).

## Outcome

- **Keep in utils**: 7 files
- **Move to owned boundaries**: 37 files
- **New tickets created**: `COH-024` to `COH-030`

## File-by-file decisions

| File | Symbols reviewed | Decision | Why | Ticket |
|---|---|---|---|---|
| `app/src/main/java/app/gamegrub/utils/FormatUtils.kt` | `FormatUtils`, `GameSourceUtils` | Split/move | UI-facing text/resource mapping should live with UI formatting/presentation boundaries, not shared utils. | `todo/COH-030.md` |
| `app/src/main/java/app/gamegrub/utils/auth/AuthUrlRedaction.kt` | `redactUrlForLogging` | Move | OAuth/security log redaction is auth boundary behavior. | `todo/COH-025.md` |
| `app/src/main/java/app/gamegrub/utils/auth/KeyAttestationHelper.kt` | `KeyAttestationHelper` | Move | Device attestation and nonce flow are security/auth responsibilities. | `todo/COH-025.md` |
| `app/src/main/java/app/gamegrub/utils/auth/PlatformAuthUtils.kt` | `PlatformAuthUtils` | Move | Cross-service sign-in aggregation is auth/session orchestration. | `todo/COH-025.md` |
| `app/src/main/java/app/gamegrub/utils/auth/PlatformOAuthHandlers.kt` | `PlatformOAuthHandlers` | Move | OAuth callback orchestration belongs to auth coordinator/use-case layer. | `todo/COH-025.md` |
| `app/src/main/java/app/gamegrub/utils/auth/PlayIntegrity.kt` | `PlayIntegrity` | Move | Integrity token lifecycle is app security infrastructure, not generic utility logic. | `todo/COH-025.md` |
| `app/src/main/java/app/gamegrub/utils/container/ContainerMigrator.kt` | `ContainerMigrator` | Move | Legacy container migration is container lifecycle ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/container/ContainerUtils.kt` | `ContainerUtils` | Move/split | Mixes container creation, defaults, storage, game-source lookup, and executable scanning; this is core container domain ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/container/LaunchDependencies.kt` | `LaunchDependencies` | Move | Launch dependency orchestration belongs to launch pipeline/domain. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/container/PreInstallSteps.kt` | `PreInstallSteps` | Move | Pre-launch installer sequencing is launch pipeline ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/game/CustomGameCache.kt` | `CustomGameCache` | Move | Cache owns custom-game identity mapping and should sit with custom-game domain. | `todo/COH-027.md` |
| `app/src/main/java/app/gamegrub/utils/game/CustomGameScanner.kt` | `CustomGameScanner` | Move/split | Owns custom-game indexing, appId generation, metadata writes, icon extraction trigger, and permission checks; this is feature-domain logic. | `todo/COH-027.md` |
| `app/src/main/java/app/gamegrub/utils/game/ExecutableSelectionUtils.kt` | `ExecutableSelectionUtils` | Move | Heuristic executable selection is custom-game launch-domain behavior. | `todo/COH-027.md` |
| `app/src/main/java/app/gamegrub/utils/game/ExeIconExtractor.kt` | `ExeIconExtractor` | Move | PE icon extraction is an importer/metadata pipeline concern for game scanning. | `todo/COH-027.md` |
| `app/src/main/java/app/gamegrub/utils/game/GameMetadataManager.kt` | `GameMetadata`, `GameMetadataManager` | Move | `.gamenative` read/write lifecycle is metadata persistence ownership. | `todo/COH-027.md` |
| `app/src/main/java/app/gamegrub/utils/general/DateTimeUtils.kt` | `DateTimeUtils` | Keep | Pure date parsing/formatting helpers with no platform/service ownership. | N/A |
| `app/src/main/java/app/gamegrub/utils/general/FlowUtils.kt` | `Flow<T>.timeChunked` | Keep | Generic coroutine/Flow extension, no app-domain coupling. | N/A |
| `app/src/main/java/app/gamegrub/utils/general/IntentLaunchManager.kt` | `IntentLaunchManager`, `TemporaryConfigStore` | Move | External launch request parsing + temporary container override state is launch gateway/coordinator ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/general/MathUtils.kt` | `MathUtils` | Keep | Pure numeric helper, no domain coupling. | N/A |
| `app/src/main/java/app/gamegrub/utils/general/NoToast.kt` | `android.widget.Toast` poison pill | Move | Build-time policy enforcement should live in explicit policy/tooling boundary, not misc utils. | `todo/COH-030.md` |
| `app/src/main/java/app/gamegrub/utils/general/StringUtils.kt` | `getAvatarURL`, `fromHtml`, `unaccent`, `getProfileUrl` | Split | `fromHtml`/`unaccent` are generic utils; persona URL builders are Steam/persona domain formatting. | `todo/COH-030.md` |
| `app/src/main/java/app/gamegrub/utils/general/UpdateInstaller.kt` | `UpdateInstaller` | Move | App update installation flow belongs to update feature boundary. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/launchdependencies/GogScriptInterpreterDependency.kt` | `GogScriptInterpreterDependency` | Move | GOG-specific dependency behavior belongs in GOG launch/install domain. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/launchdependencies/LaunchDependency.kt` | `LaunchDependency`, `LaunchDependencyCallbacks` | Move | Launch dependency contract belongs to launch domain package. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/manifest/ManifestComponentHelper.kt` | `ManifestComponentHelper` | Move/split | Owns installed component discovery + DXVK option logic; this is component/content domain ownership. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/manifest/ManifestInstaller.kt` | `ManifestInstaller`, `ManifestInstallResult` | Move | Manifest-driven download/install orchestration belongs to component install domain. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/manifest/ManifestModels.kt` | `ManifestEntry`, `ManifestData`, `ManifestContentTypes` | Move | Manifest schema should live with component manifest repository/installer boundary. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/manifest/ManifestRepository.kt` | `ManifestRepository` | Move | Manifest fetch/cache policy ownership belongs to component manifest feature package. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/network/UpdateChecker.kt` | `UpdateInfo`, `UpdateChecker` | Move | App update-check feature logic should be under update boundary, not generic network utils. | `todo/COH-028.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/GogScriptInterpreterStep.kt` | `GogScriptInterpreterStep` | Move | GOG launch preinstall logic belongs with launch/GOG domain. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/OpenALStep.kt` | `OpenALStep` | Move | Pre-launch redistributable installation is launch pipeline ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/PhysXStep.kt` | `PhysXStep` | Move | Pre-launch redistributable installation is launch pipeline ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/PreInstallStep.kt` | `PreInstallStep` | Move | Contract belongs with launch preinstall domain package. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/VcRedistStep.kt` | `VcRedistStep` | Move | Pre-launch redistributable installation is launch pipeline ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/preInstallSteps/XnaFrameworkStep.kt` | `XnaFrameworkStep` | Move | Pre-launch redistributable installation is launch pipeline ownership. | `todo/COH-026.md` |
| `app/src/main/java/app/gamegrub/utils/steam/KeyValueUtils.kt` | KeyValue mapping extensions | Move | Steam appinfo parsing belongs to Steam adapter/parser boundary. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/LicenseSerializer.kt` | `LicenseSerializer` | Move | Steam license/manifest serialization belongs to Steam persistence/auth boundary. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamClientFilesManager.kt` | `SteamClientFilesManager` | Move | Steam client binary backup/restore is Steam install/runtime ownership. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamControllerVdfUtils.kt` | `SteamControllerVdfUtils` + VDF parser types | Move | Steam Input VDF parsing/mapping is Steam input domain logic. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamFormatUtils.kt` | `SteamFormatUtils` | Move | Steam-specific formatting should live in Steam package, not generic utils. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamManifestInstaller.kt` | `SteamManifestInstaller` | Move | ACF generation and depot manifest writing are Steam install-domain concerns. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamSaveLocationManager.kt` | `SteamSaveLocationManager` | Move | Special-case save symlink handling is Steam save/runtime ownership. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamTokenHelper.kt` | `SteamTokenHelper` | Move | Steam ticket obfuscation/deobfuscation is Steam auth/crypto boundary logic. | `todo/COH-029.md` |
| `app/src/main/java/app/gamegrub/utils/steam/SteamUtils.kt` | `SteamUtils` | Move/split | Large multi-owner facade (Steam DLL replacement, manifests, save mapping, achievements, config writes); should be decomposed into Steam domains/managers. | `todo/COH-029.md` |

## Keep-in-utils notes

These stay in `utils` because they are cross-cutting and not feature-owned:
- `app/src/main/java/app/gamegrub/utils/general/DateTimeUtils.kt`
- `app/src/main/java/app/gamegrub/utils/general/FlowUtils.kt`
- `app/src/main/java/app/gamegrub/utils/general/MathUtils.kt`
- Generic subset of `app/src/main/java/app/gamegrub/utils/general/StringUtils.kt` (`fromHtml`, `unaccent`)

## Ticket map

- Parent tracking: `todo/COH-024.md`
- Auth/security relocation: `todo/COH-025.md`
- Container/launch dependency relocation: `todo/COH-026.md`
- Custom game domain relocation: `todo/COH-027.md`
- Manifest/update relocation: `todo/COH-028.md`
- Steam utilities decomposition: `todo/COH-029.md`
- Shared formatting/policy split: `todo/COH-030.md`

