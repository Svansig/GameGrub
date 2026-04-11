# JNI Boundary Remediation Backlog

Date: 2026-04-11

## Now

### 1) JNI-VIRGL-MEASURE-003 - Capture sustained in-session VirGL JNI metrics on a virgl-stable title/session
- **Problem statement**: VirGL is now selected correctly, but the tested title exits quickly under virgl and live-window counters remain inactive/near-zero.
- **Affected files**: `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java`, `app/src/main/java/app/gamegrub/debug/JniStatsDumpReceiver.kt`
- **Why it matters**: Flush callback reduction and deterministic EGL lifecycle work cannot be ranked without non-zero in-session request/flush data.
- **Evidence level**: high (runtime logs confirm virgl selected; current title still fails to provide sustained sampling window)
- **Recommended next action**: Run the same measurement workflow against a virgl-stable title/container path and capture startup + 30s active-window dumps.
- **Complexity**: S
- **Validation plan**: Require `Graphics Driver: virgl` and non-zero `VirGL JNI perf requests/flush` counters in live dumps.

### 2) JNI-EXIT-DEBUG-003 - Add deterministic debug exit trigger for teardown-time metric flush
- **Problem statement**: Shell-driven `BACK`/confirm automation did not reliably trigger `XServerExitCoordinator.requestExit(...)`, so stop-time logs are still awkward to collect.
- **Affected files**: `app/src/main/java/app/gamegrub/ui/screen/xserver/XServerExitCoordinator.kt`, `app/src/main/java/app/gamegrub/debug/JniStatsDumpReceiver.kt`, `app/src/main/AndroidManifest.xml`
- **Why it matters**: A repeatable teardown trigger would make stop-time `XConnectorEpoll` / `VirGLRendererComponent` dumps and `[Teardown]` summaries scriptable.
- **Evidence level**: medium/high (real device automation attempt failed repeatedly)
- **Recommended next action**: Add one adb-invokable debug action that requests clean session exit.
- **Complexity**: S
- **Validation plan**: Invoke the debug action during an active session and verify `[Teardown]`, `Native JNI perf`, and `VirGL JNI perf` logs appear without manual UI interaction.

## Next

### 0) JNI-VIRGL-MEASURE-002 - Fix temporary graphics-driver override propagation so VirGL can actually be measured
- **Status**: Completed in this pass.
- **Outcome**: External launch now resolves to virgl (`[RendererSelection]` chain and `Graphics Driver: virgl` confirmed on device).
- **Documentation**: `docs/audits/virgl-launch-override-validation.md`

### 3) JNI-VIRGL-002 - Dedicated refactor for deterministic EGL context availability
- **Problem statement**: Current pass reduced wait risk, but a full deterministic lifecycle may require broader renderer ownership changes.
- **Affected files**: `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java`, `app/src/main/cpp/virglrenderer/server/virgl_server.c`, `app/src/main/cpp/virglrenderer/server/virgl_server_renderer.c`
- **Why it matters**: Shared-context wait latency can still appear under UI/GL thread contention.
- **Evidence level**: medium (architectural risk identified; runtime tails pending)
- **Recommended next action**: Define an explicit context-ready contract at component startup and remove connection-time wait path.
- **Complexity**: M
- **Validation plan**: prove zero waits in connection path and no EGL context nulls across connection bursts.

### 4) JNI-VIRGL-001 - Collect runtime VirGL perf snapshots once a VirGL-capable session exists
- **Problem statement**: VirGL request/flush/shared-context wait counters are present but remained inactive in the measured wrapper-renderer session.
- **Affected files**: `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java`
- **Why it matters**: Need request/flush/shared-EGL numbers before selecting between VirGL flush reduction and EGL lifecycle work.
- **Evidence level**: medium/high (instrumentation exists, current title did not exercise it)
- **Recommended next action**: Re-run the same measurement workflow after `Graphics Driver: virgl` is confirmed.
- **Complexity**: S
- **Validation plan**: Capture `VirGL JNI perf: requests{...} flush{...} sharedEglWait{...}` from a live session and compare startup vs steady-state windows.

## Later

### 5) JNI-VIRGL-003 - Evaluate callback-frequency reduction for frontbuffer updates
- **Problem statement**: `flushFrontbuffer` can be very frequent; callback volume may still be meaningful overhead.
- **Affected files**: `app/src/main/cpp/virglrenderer/server/virgl_server_renderer.c`, `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java`
- **Why it matters**: Potential JNI crossing reduction opportunity if measurement confirms heavy callback churn.
- **Evidence level**: medium (still unquantified because current measured session used wrapper renderer)
- **Recommended next action**: decide based on collected perf snapshots; if warranted, prototype batched/shared-state signaling.
- **Complexity**: M/L
- **Validation plan**: compare frame pacing and callback counts before/after prototype under identical scene.

### 6) JNI-XCONNECTOR-002 - Add sampled JNI exception-check telemetry on callback boundaries
- **Problem statement**: Callback exception propagation remains lightly guarded.
- **Affected files**: `app/src/main/cpp/winlator/xconnector_epoll.c`
- **Why it matters**: Hidden Java exceptions can invalidate native callback assumptions.
- **Evidence level**: medium (risk known, no failure captured)
- **Recommended next action**: Add debug-only sampled `ExceptionCheck` counter after callback invocations.
- **Complexity**: S
- **Validation plan**: verify zero exception counts during normal scenarios and actionable logs during fault injection.

## Rejected / Not Justified Yet

### 7) JNI-XCONNECTOR-REJECT-001 - Additional epoll JNI micro-optimization pass
- **Problem statement**: Another epoll tightening pass was a candidate after caching/log cleanup.
- **Affected files**: `app/src/main/cpp/winlator/xconnector_epoll.c`, `app/src/main/java/com/winlator/xconnector/XConnectorEpoll.java`
- **Why it matters**: Could reduce JNI overhead if callback or lookup costs were still material.
- **Evidence level**: medium/high
- **Recommended next action**: do not prioritize now.
- **Complexity**: S/M
- **Validation plan**: Revisit only if a future title shows materially higher callback rates or non-zero lookup misses after warmup.

**Reason rejected for now**:
- Real device measurements showed cache misses drop to zero after warmup.
- `recvAncillaryMsg` averaged only `25-38us` in the measured session.
- `epoll avg_us` is dominated by blocking wait time, not proof of CPU-side JNI overhead.

### 8) JNI-BUFFER-REJECT-002 - GPUImage / ByteBuffer follow-up work
- **Problem statement**: Prior audit suggested possible ByteBuffer-related follow-up.
- **Affected files**: `app/src/main/cpp/winlator/gpu_image.c`, `app/src/main/cpp/extras/gpu_image.c`
- **Why it matters**: Could matter if active renderer measurements pointed there.
- **Evidence level**: low for current pass
- **Recommended next action**: none until a VirGL-capable measurement run implicates it.
- **Complexity**: M
- **Validation plan**: N/A until a relevant renderer path is measured.

### 9) JNI-CALLBACK-REJECT-003 - Broad callback/global-ref lifecycle rewrite
- **Problem statement**: Generic JNI callback/global-ref cleanup was previously proposed.
- **Affected files**: broad native callback sites
- **Why it matters**: Could reduce correctness risk if concrete misuse exists.
- **Evidence level**: low for current pass
- **Recommended next action**: none until an actual stored-ref or cross-thread misuse case is shown.
- **Complexity**: M/L
- **Validation plan**: N/A until a concrete misuse is identified.

### 10) JNI-ARCH-REJECT-001 - Migrate full X11 request handling across JNI boundary
- **Problem statement**: Previous audit suggested broad boundary migration for performance.
- **Affected files**: broad (`xconnector`, request handlers, X11 stack)
- **Why it matters**: High rewrite risk with unclear measured gain.
- **Evidence level**: low for this pass
- **Recommended next action**: none until strong benchmark evidence exists.
- **Complexity**: L
- **Validation plan**: N/A (rejected for current evidence level)

### 11) JNI-ARCH-REJECT-002 - Migrate full VirGL protocol ownership to native as immediate action
- **Problem statement**: Broad migration was proposed without this pass's supporting metrics.
- **Affected files**: broad VirGL managed/native boundary
- **Why it matters**: High complexity and correctness risk.
- **Evidence level**: low for immediate execution
- **Recommended next action**: revisit only after instrumentation-driven bottleneck confirmation.
- **Complexity**: L
- **Validation plan**: N/A (deferred pending stronger evidence)

### 12) JNI-BUFFER-REJECT-004 - Reflection-based DirectByteBuffer address mutation hack
- **Problem statement**: Proposed as allocation avoidance strategy.
- **Affected files**: hypothetical only
- **Why it matters**: brittle, non-portable, and correctness-risky.
- **Evidence level**: rejected by design constraints
- **Recommended next action**: do not pursue.
- **Complexity**: M
- **Validation plan**: N/A

