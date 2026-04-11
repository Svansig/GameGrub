# VirGL Launch Override Validation

Date: 2026-04-11

## Root cause summary

External launch intents were parsed correctly, and `container_config` included `graphicsDriver=virgl`, but runtime still resolved to `wrapper` during `XServerScreen` setup.

The main issue was split propagation:

1. `LaunchRequestGatewayImpl` called `LaunchRequestManager.handleLaunchIntent(...)` without wiring the `applyTemporaryConfig` callback.
2. For `onNewIntent` launches (`isNewIntent=true`), no temporary override was applied before `ExternalGameLaunch` emission.
3. `XServerScreen` loaded container state using `ContainerUtils.getContainer(context, appId)` and ignored temporary overrides even when they existed.

## Files/symbols changed

- `app/src/main/java/app/gamegrub/gateway/impl/LaunchRequestGatewayImpl.kt`
  - `handleLaunchIntent(...)` now passes `applyTemporaryConfig` and applies `IntentLaunchManager.applyTemporaryConfigOverride(...)` when override config exists.
- `app/src/main/java/app/gamegrub/LaunchRequestManager.kt`
  - Added structured intent-stage renderer log: `[RendererSelection][Intent] ... requestedDriver=...`.
- `app/src/main/java/app/gamegrub/ui/launch/GameLaunchOrchestrator.kt`
  - Added structured pre-launch resolution log: `[RendererSelection][PreLaunch] ... requestedDriver/resolvedDriver/source`.
- `app/src/main/java/app/gamegrub/ui/screen/xserver/XServerScreen.kt`
  - Uses `ContainerUtils.getOrCreateContainerWithOverride(...)` when `IntentLaunchManager.hasTemporaryOverride(appId)`.
  - Added `[RendererSelection][XServerScreen] ...` confirmation log.
- `app/src/main/java/app/gamegrub/container/launch/env/EnvironmentSetupCoordinator.kt`
  - Added `[RendererSelection][EnvironmentSetup] ... xServerStateDriver/containerDriver` log.

## Validation procedure

```bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
./gradlew :app:installDebug
$ADB shell am force-stop app.gamegrub
$ADB logcat -c
$ADB shell am start -W -n app.gamegrub/.MainActivity
sleep 10
$ADB shell am broadcast -a app.gamegrub.DEBUG_DUMP_JNI_STATS -n app.gamegrub/.debug.JniStatsDumpReceiver --ez reset true --es scenario pre_virgl_reset
$ADB shell am start -W -a app.gamegrub.LAUNCH_GAME -n app.gamegrub/.MainActivity --ei app_id 250760 --es game_source STEAM --es container_config '{"graphicsDriver":"virgl"}'
sleep 25
$ADB shell am broadcast -a app.gamegrub.DEBUG_DUMP_JNI_STATS -n app.gamegrub/.debug.JniStatsDumpReceiver --ez reset true --es scenario virgl_session_start
sleep 30
$ADB shell am broadcast -a app.gamegrub.DEBUG_DUMP_JNI_STATS -n app.gamegrub/.debug.JniStatsDumpReceiver --ez reset false --es scenario virgl_active_window
$ADB logcat -d -v brief | grep -E 'RendererSelection|Graphics Driver:|JniStatsDumpReceiver|VirGL JNI perf|XConnectorEpoll|VirGLRendererComponent|Teardown'
```

## Validation evidence

### Before final fix

- `[RendererSelection][PreLaunch] ... requestedDriver=virgl resolvedDriver=virgl source=temporary_override`
- `[RendererSelection][EnvironmentSetup] ... xServerStateDriver=wrapper containerDriver=wrapper`
- `Graphics Driver: wrapper`

This showed override was present in pre-launch but dropped by runtime container selection in `XServerScreen`.

### After final fix

- `[RendererSelection][Intent] appId=STEAM_250760 requestedDriver=virgl isNewIntent=true consumePending=false`
- `[RendererSelection][Intent] appId=STEAM_250760 applied temporary container override`
- `[RendererSelection][PreLaunch] appId=STEAM_250760 requestedDriver=virgl resolvedDriver=virgl source=temporary_override`
- `[RendererSelection][XServerScreen] appId=STEAM_250760 using temporary override container`
- `[RendererSelection][EnvironmentSetup] appId=STEAM_250760 xServerStateDriver=virgl containerDriver=virgl`
- `Graphics Driver: virgl`

This confirms external launch now resolves the effective runtime driver to VirGL.

## JNI measurement outcome for this virgl run

- VirGL component activated (`VirGLRendererComponent` start + `.virgl/V0` connector startup present).
- Session exited quickly in this title/config combination; teardown logs were emitted.
- Stop-time VirGL perf line was emitted but remained near-zero:
  - `VirGL JNI perf: requests{calls=0,sampled=0,avgUs=0} flush{calls=0,sampled=0,avgUs=0} sharedEglWait{calls=0,avgUs=0}`
- Live `JniStatsDumpReceiver` snapshots captured after teardown therefore reported `VirGL JNI perf: inactive`.

## Remaining risks / edge cases

- The selected test title (`STEAM_250760`) under VirGL appears to terminate quickly on this device/build; this blocks sustained active-window VirGL callback sampling.
- Follow-up should use a title or container path that remains running long enough under VirGL to populate `requests`/`flush` metrics in-session.

## Documentation impact

Documentation Impact: Added root-cause trace, concrete propagation fix, and on-device validation evidence for external VirGL override resolution.
