# Shovel Knight Audio and Exit Analysis (2026-04-09)

## Root cause (no audio)

Wine was launched inside the imagefs rootfs (`proot --rootfs=<...>/imagefs`) but `PULSE_SERVER` was exported as an Android host path:

- previous value: `/data/user/0/app.gamegrub/files/imagefs/tmp/.sound/PS0`

Inside proot, Wine should connect to the guest-visible socket path under `/tmp`. The host absolute path is not guaranteed to be reachable from the guest namespace, which causes winepulse/mmdevapi initialization to fail with:

- `err:mmdevapi:init_driver No driver from L"pulse" could be initialized`

## Implemented fix

1. `PULSE_SERVER` is now exported as guest path:

- `unix:/tmp/.sound/PS0`

2. Added PulseAudio startup diagnostics:

- logs pulseaudio launch socket/arch
- waits briefly for socket creation and logs if socket is not ready
- logs missing modules directory

## Status 137 interpretation

In this run, status `137` is consistent with internal teardown (`SIGKILL`) after session exit handling, not necessarily with initial launch failure.

To avoid misleading failure messages, launch-error reporting now suppresses status `137` when internal exit has already started.

## Documentation Impact

This document records the launch audio root cause, minimal patch scope, and status-137 reporting behavior changes for future regressions.

