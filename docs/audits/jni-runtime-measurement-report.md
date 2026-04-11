# JNI Runtime Measurement Report

Date: 2026-04-11

## Test environment

- **Device**: Odin2 (`adb devices -l` reported `model:Odin2 device:kalama`)
- **OS**: Android 13
- **Build**: `debug`
- **Package**: `app.gamegrub`
- **Install command**: `./gradlew :app:installDebug`
- **ADB used for capture**: `~/Android/Sdk/platform-tools/adb`
- **Display during capture**: 1920x1080 landscape (`dumpsys activity top`)

## What was verified in code before measuring

### XConnector stop-time dump

- **File/symbol**: `app/src/main/java/com/winlator/xconnector/XConnectorEpoll.java:97-124`, `stop()`
- **Behavior**:
  - Runs when the connector is stopped.
  - Dumps `getNativePerfStats(true)` through `Timber.tag("XConnectorEpoll").d(...)`.
  - Reset behavior is `true`, so counters are cleared after emission.
- **Build condition**: guarded by `BuildConfig.DEBUG`.
- **Sink**: logcat (Timber tag `XConnectorEpoll`).

### VirGL stop-time dump

- **File/symbol**: `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:54-64`, `stop()`
- **Behavior**:
  - Runs when the renderer component is stopped.
  - Dumps `perfStats.dumpAndReset()` through `Timber.tag("VirGLRendererComponent").d(...)`.
  - Reset behavior is `true`.
- **Build condition**: guarded by `BuildConfig.DEBUG`.
- **Sink**: logcat (Timber tag `VirGLRendererComponent`).

### Environment stop path that triggers component dumps

- **File/symbol**: `app/src/main/java/app/gamegrub/ui/screen/xserver/XServerExitCoordinator.kt:101-103`
- **Call path**:
  1. `XServerExitCoordinator.requestExit(...)`
  2. `environment?.stopEnvironmentComponentsWithSummary()`
  3. `XEnvironment.stopEnvironmentComponentsWithSummary()`
  4. Per-component `stop()` calls
  5. Connector / VirGL stop-time logs emitted if debug build and those components exist
- **Sink**: logcat via Timber, plus teardown summary line `[Teardown]...`

### Live on-demand dump path added for measurement reliability

The regular stop path was not shell-reproducible enough on this device, so a minimal debug-only live dump hook was added.

- **Receiver**: `app/src/main/java/app/gamegrub/debug/JniStatsDumpReceiver.kt`
- **Manifest registration**: `app/src/main/AndroidManifest.xml:135-142`
- **Action**: `app.gamegrub.DEBUG_DUMP_JNI_STATS`
- **Behavior**:
  - Reads global native XConnector stats with `XConnectorEpoll.getNativePerfStats(reset)`
  - Reads active VirGL stats from the current runtime environment when `VirGLRendererComponent` exists
  - Emits a single structured line to logcat under tag `JniStatsDumpReceiver`
- **Build condition**: receiver exits early unless `BuildConfig.DEBUG`
- **Sink**: logcat (Timber tag `JniStatsDumpReceiver`)

### Small measurement blocker fix applied during this pass

External-launch measurement was blocked by a crash where `XServerScreen` received an empty app id.

- **Crash evidence**:
  - `ComposeInternal` / `AndroidRuntime` reported `java.lang.Exception: Container does not exist for game` from `XServerScreen.kt:162`
  - Stack reached `GameGrubMain.kt:1220`
- **Fix**:
  - `app/src/main/java/app/gamegrub/ui/model/MainViewModel.kt:447-451`
  - `launchApp(context, appId)` now reasserts `setLaunchedAppId(appId)` before navigation
- **Reason**: required to make the measured session actually start

## Exact collection procedure

### Log capture policy

- Clear logcat before each scenario.
- Use one scenario label per live dump.
- Capture only the relevant tags/patterns:
  - `JniStatsDumpReceiver`
  - `XConnectorEpoll`
  - `VirGLRendererComponent`
  - `\[Teardown\]`
  - `EnvironmentSetupCoordinator`

### Commands used

```bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
./gradlew :app:installDebug
```

#### Idle shell snapshot

```bash
$ADB shell am force-stop app.gamegrub
$ADB logcat -c
$ADB shell cmd statusbar collapse
$ADB shell am start -W -n app.gamegrub/.MainActivity
sleep 15
$ADB shell am broadcast \
  -a app.gamegrub.DEBUG_DUMP_JNI_STATS \
  -n app.gamegrub/.debug.JniStatsDumpReceiver \
  --ez reset false \
  --es scenario idle_shell
$ADB logcat -d -v brief | grep 'JniStatsDumpReceiver'
```

#### Menu navigation snapshot

```bash
$ADB shell am force-stop app.gamegrub
$ADB logcat -c
$ADB shell cmd statusbar collapse
$ADB shell am start -W -n app.gamegrub/.MainActivity
sleep 12
$ADB shell input swipe 900 800 300 800 300
sleep 2
$ADB shell input swipe 300 800 900 800 300
sleep 2
$ADB shell input swipe 960 820 960 420 300
sleep 2
$ADB shell input swipe 960 420 960 820 300
sleep 2
$ADB shell am broadcast \
  -a app.gamegrub.DEBUG_DUMP_JNI_STATS \
  -n app.gamegrub/.debug.JniStatsDumpReceiver \
  --ez reset false \
  --es scenario menu_navigation
$ADB logcat -d -v brief | grep 'JniStatsDumpReceiver'
```

#### Session-start + active-render window snapshots

```bash
$ADB shell am force-stop app.gamegrub
$ADB logcat -c
$ADB shell cmd statusbar collapse
$ADB shell am start -W -n app.gamegrub/.MainActivity
sleep 12
$ADB shell input keyevent 66
sleep 2
$ADB shell input keyevent 4
sleep 2
$ADB shell am broadcast \
  -a app.gamegrub.DEBUG_DUMP_JNI_STATS \
  -n app.gamegrub/.debug.JniStatsDumpReceiver \
  --ez reset true \
  --es scenario pre_session_reset
sleep 1
$ADB shell am start -W \
  -a app.gamegrub.LAUNCH_GAME \
  -n app.gamegrub/.MainActivity \
  --ei app_id 250760 \
  --es game_source STEAM
sleep 20
$ADB shell am broadcast \
  -a app.gamegrub.DEBUG_DUMP_JNI_STATS \
  -n app.gamegrub/.debug.JniStatsDumpReceiver \
  --ez reset true \
  --es scenario session_start
sleep 30
$ADB shell am broadcast \
  -a app.gamegrub.DEBUG_DUMP_JNI_STATS \
  -n app.gamegrub/.debug.JniStatsDumpReceiver \
  --ez reset false \
  --es scenario active_render_window
$ADB logcat -d -v brief | grep 'JniStatsDumpReceiver'
```

#### Correlation checks

```bash
$ADB logcat -d -v brief | grep -E 'EnvironmentSetupCoordinator|XConnectorEpoll|VirGLRendererComponent|ShovelKnight'
$ADB shell ps -A | grep -E 'app\.gamegrub|ShovelKnight|wineserver'
```

## Scenarios executed

### 1. App launch to idle shell
- **Action**: launch app and wait on home shell
- **Approx duration**: 15s
- **Expected subsystem coverage**: UI shell only
- **Observed**: no JNI/X11/VirGL activity

### 2. Menu navigation / non-game interaction
- **Action**: launch app, horizontal and vertical swipes on home UI, then dump
- **Approx duration**: 20s
- **Expected subsystem coverage**: Compose/navigation only
- **Observed**: no JNI/X11/VirGL activity

### 3. Starting a container/session
- **Action**: external launch of `STEAM_250760` (Shovel Knight), dump after 20s
- **Approx duration**: 20s measurement window after reset
- **Expected subsystem coverage**: launch pipeline, X11/XConnector, Wine/game process startup
- **Observed**:
  - `EnvironmentSetupCoordinator` logged `Graphics Driver: wrapper`
  - `XConnectorEpoll` started X11 and SysVSHM connectors
  - `ShovelKnight.exe` and `wineserver` processes were alive

### 4. Active rendering scenario
- **Action**: continue same running session for another 30s, dump again
- **Approx duration**: 30s steady-state window
- **Expected subsystem coverage**: active runtime X11 traffic during gameplay
- **Observed**:
  - continued XConnector activity
  - VirGL remained inactive because the measured title/session used `Graphics Driver: wrapper`

### 5. Shutdown / component stop
- **Action attempted**: `BACK` / `ENTER` exit automation
- **Observed**: shell automation did not trigger `XServerExitCoordinator.requestExit(...)` reliably on this device, so stop-time flush lines were not reproducibly emitted
- **Fallback used**: live dump receiver immediately before scenario end / reset

## Raw metrics by scenario

| Scenario | Window | Renderer | epoll calls | epoll events | JNI callbacks | recvAnc calls | recvAnc avg us | recvAnc fds | sendAnc calls | sendAnc avg us | waitRead calls | lookupMiss new/existing/ancillary | VirGL |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|
| `idle_shell` | 15s | N/A | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0/0/0 | inactive |
| `menu_navigation` | 20s | N/A | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0/0/0 | inactive |
| `session_start` | 20s | `wrapper` | 1424 | 1442 | 1442 | 1416 | 25 | 4 | 2 | 16 | 0 | 1/1/1 | inactive |
| `active_render_window` | 30s | `wrapper` | 2654 | 2654 | 2654 | 2654 | 38 | 0 | 0 | 0 | 0 | 0/0/0 | inactive |
| `wrapper_session_live` | 50s | `wrapper` | 4351 | 4389 | 4389 | 4363 | 33 | 4 | 2 | 13 | 0 | 1/1/1 | inactive |

## Derived metrics

| Scenario | callback/s | recvAnc total CPU-ish time | epoll wall total | Notes |
|---|---:|---:|---:|---|
| `session_start` | 72.1/s | 35.4 ms over 20s | 22.68 s | startup window includes first ancillary FDs and first lookup misses |
| `active_render_window` | 88.5/s | 100.9 ms over 30s | 31.08 s | steady-state active gameplay window |
| `wrapper_session_live` | 87.8/s | 144.0 ms over 50s | 52.48 s | full live wrapper session |

### Interpretation of derived metrics

- `epoll avg_us` is **wall time**, not pure CPU time. In this loop it is dominated by blocking wait behavior, so the large `avg_us` values do **not** imply equivalent JNI overhead.
- `recvAncillaryMsg` is the best measured JNI-adjacent hot path in this run. Even there, total measured time stayed small relative to scenario duration.
- JNI lookup cache misses dropped to **zero after warmup** in the steady-state window.
- `waitForSocketRead` did not execute in these runs, so multithreaded-client wait behavior was not exercised.
- VirGL metrics stayed inactive because the measured title launched under `Graphics Driver: wrapper`.

## Top measured hotspots

### 1. XConnector callback volume under active gameplay
- `active_render_window`: **2654 callbacks / 30s** = **88.5 callbacks/s**
- This is the highest sustained JNI-adjacent event rate actually measured.
- However, it is not yet evidence that callback reduction is needed by itself.

### 2. `recvAncillaryMsg` frequency, not duration
- `active_render_window`: **2654 calls**, **38 us average**
- `wrapper_session_live`: **4363 calls**, **33 us average**
- The path is hot by count, but measured per-call cost is small.

### 3. epoll loop wall time is dominated by blocking wait
- `session_start`: **15,927 us avg**
- `active_render_window`: **11,709 us avg**
- Because this timing includes the wait, it points away from JNI lookup/log overhead as the next bottleneck.

## Non-issues / disproven concerns

### JNI lookup churn in XConnector
- **Disproven as ongoing hot cost after warmup** in this measured session.
- Evidence: `lookupMiss{new=0,existing=0,ancillary=0}` during the 30s steady-state window.

### Menu/idle JNI pressure
- **Not present**.
- Both non-game scenarios stayed at zero across all captured counters.

### `waitForSocketRead` as a current issue
- **Not exercised** in this session.
- No evidence to prioritize it.

### VirGL callback / shared EGL concerns for this specific measured title
- **Unmeasured in this run** because VirGL never became active.
- Evidence: every dump reported `VirGL JNI perf: inactive` and launch logs reported `Graphics Driver: wrapper`.

### Generic global-ref / `GetDirectBufferAddress` rewrites
- **Still not justified**.
- This pass gathered no evidence pointing to those areas.

## Interpretation

1. The XConnector JNI fixes from the previous pass are behaving as intended:
   - cache misses vanish after warmup
   - no sign that lookup churn remains a runtime issue
2. The dominant epoll timing metric is mostly telling us about blocking wait behavior, not useful CPU burn.
3. The active measured JNI-adjacent cost is the frequency of X11 callback/ancillary traffic, but the measured per-call cost remains small enough that another epoll micro-optimization pass is not well justified.
4. The major unresolved JNI candidate remains **VirGL**, but this device/title combination did not exercise it.
5. An attempted `container_config` override with `{"graphicsDriver":"virgl"}` still launched with `Graphics Driver: wrapper`, so VirGL measurement is currently blocked by launch/config behavior rather than by missing counters.

## VirGL override validation update (same day follow-up run)

### What changed before rerun

- External-launch override application was wired in `LaunchRequestGatewayImpl.handleLaunchIntent(...)` via `applyTemporaryConfig` callback.
- `XServerScreen` now materializes container state with temporary override when present.
- Structured renderer logs were added at intent, pre-launch, XServer-screen, and environment-setup stages.

### New virgl run evidence

- Intent stage: `[RendererSelection][Intent] ... requestedDriver=virgl`
- Pre-launch stage: `[RendererSelection][PreLaunch] ... requestedDriver=virgl resolvedDriver=virgl source=temporary_override`
- XServer stage: `[RendererSelection][XServerScreen] ... using temporary override container`
- Environment stage: `[RendererSelection][EnvironmentSetup] ... xServerStateDriver=virgl containerDriver=virgl`
- Final launch log: `Graphics Driver: virgl`

This confirms the external launch override now propagates to effective runtime driver selection.

### New raw metrics (virgl validation run)

| Scenario | Window | Renderer | XConnector (global dump) | VirGL |
|---|---:|---|---|---|
| `pre_virgl_reset` | baseline | N/A | all zeros after reset | inactive |
| `virgl_session_start` | 25s target | `virgl` selected | `epoll=0 callbacks=0` at live dump point | inactive at live dump |
| `virgl_active_window` | +30s target | `virgl` selected | `epoll=0 callbacks=0` at live dump point | inactive at live dump |

Additional stop-time logs from the same run:

- VirGL connector stop dump: `epoll{calls=1,avg_us=142247,events=1,callbacks=1}`
- VirGL perf stop dump: `requests{calls=0,sampled=0,avgUs=0} flush{calls=0,sampled=0,avgUs=0} sharedEglWait{calls=0,avgUs=0}`
- Teardown summary emitted: `[Teardown]...[total=41ms]...`

### Interpretation of the virgl update

1. The launch/config propagation bug is fixed: runtime now selects VirGL when requested.
2. The selected test title (`STEAM_250760`) exits quickly under this virgl run, so live-window VirGL counters are still not meaningfully populated.
3. We now have correct renderer selection evidence, but still need a sustained virgl-active title/session to rank `flushFrontbuffer` vs EGL lifecycle work.

## Recommended next implementation target

**Run the same measurement workflow on a virgl-stable title/container session to capture non-zero in-session VirGL request/flush counters.**

Why:
- Driver-selection propagation is now verified fixed.
- Current virgl run did not produce sustained in-session callback data.
- XConnector measurements remain stable and do not justify another immediate JNI micro-optimization pass.

## Documentation impact

Documentation Impact: Added real device runtime measurements, exact collection procedure, and a measured recommendation for the next JNI-focused change set.
