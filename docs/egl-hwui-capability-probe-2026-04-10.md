# EGL/HWUI Capability Probe (2026-04-10)

## Why

`OpenGLRenderer` can log `Unable to match the desired swap behavior.` without exposing which swap path failed.

To make this diagnosable from app logs, GameGrub now captures startup EGL capabilities related to HWUI swap behavior fallback.

## What it logs

At app startup (debug builds), `EglHwuiCapabilityProbe` logs:

- EGL vendor and version
- swap-relevant extension support:
  - `EGL_EXT_buffer_age`
  - `EGL_KHR_partial_update`
  - `EGL_KHR_swap_buffers_with_damage`
  - `EGL_EXT_swap_buffers_with_damage`
- window config totals and capability counts:
  - number of window-capable EGL configs
  - count with `EGL_SWAP_BEHAVIOR_PRESERVED_BIT`
  - count with `EGL_BUFFER_PRESERVED` (when queryable)
  - count with ES3 renderable bit
- first five sample config summaries (surface flags, alpha size, renderable bits, swap behavior)

## Where

- Probe implementation: `app/src/main/java/app/gamegrub/graphics/diagnostics/EglHwuiCapabilityProbe.kt`
- Startup hook: `app/src/main/java/app/gamegrub/MainActivity.kt`

## How to use

Capture logs while cold starting the app and reproducing the warning:

```bash
adb logcat -v time | grep -E "OpenGLRenderer|HWUI|AdrenoGLES|GraphicsEnvironment"
```

Interpretation guidance:

- If `OpenGLRenderer` warns but probe shows preserved/damage support is missing, fallback is expected driver behavior.
- If support exists but warning persists with jank, investigate startup frame pressure and route-specific rendering load.
- If warnings correlate with external display or renderer moves, include `XServerView` and swap-controller logs in the same capture.

## Documentation Impact

Added focused diagnostics documentation for EGL/HWUI swap fallback investigation.

