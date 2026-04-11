# JNI Boundary Follow-up Pass (Execution)

Date: 2026-04-11

## Scope

This pass focuses on small, evidence-driven JNI hardening in known hotspots. It intentionally avoids speculative boundary migrations.

## Re-Verification of Prior Audit Claims

### 1) Repeated JNI method/class lookups in `xconnector_epoll.c`

- **Classification**: proven hot-path issue
- **Evidence (before this pass)**:
  - `app/src/main/cpp/winlator/xconnector_epoll.c:131-135` called `GetObjectClass` + `GetMethodID` inside `doEpollIndefinitely(...)`.
  - `app/src/main/cpp/winlator/xconnector_epoll.c:228-229` did the same in `ClientSocket_recvAncillaryMsg(...)`.
  - `app/src/main/cpp/winlator/xconnector_epoll.c:288-289` did the same in `waitForSocketRead(...)`.
- **Action**:
  - Added cached `jmethodID` slots and helper lookup path in `app/src/main/cpp/winlator/xconnector_epoll.c:57-88`.
  - Hot callbacks now use cached IDs in:
    - `doEpollIndefinitely(...)` (`app/src/main/cpp/winlator/xconnector_epoll.c:193-230`)
    - `recvAncillaryMsg(...)` (`app/src/main/cpp/winlator/xconnector_epoll.c:310-329`)
    - `waitForSocketRead(...)` (`app/src/main/cpp/winlator/xconnector_epoll.c:399-409`)

### 2) Hot-path native logging in epoll/render-related paths

- **Classification**: proven hot-path issue for epoll; likely issue in some render paths
- **Evidence (epoll)**:
  - Previous audit hotspots (`accept`, ancillary FD path, `sendmsg`) were in the event socket loop.
- **Action**:
  - Replaced raw `printf` macro usage in `xconnector_epoll.c` with compile-gated trace macro and retained error-level logging:
    - Trace/error macros: `app/src/main/cpp/winlator/xconnector_epoll.c:20-27`
  - Removed per-event hot logs in epoll request path by deleting those call sites.

### 3) Blocking wait/synchronous callback in `VirGLRendererComponent.getSharedEGLContext()`

- **Classification**: architectural issue with direct latency risk
- **Evidence**:
  - Existing code queued GL-thread work then blocked caller with wait/notify.
  - Native side requests `getSharedEGLContext` through `GetMethodID(..., "()J")` in `app/src/main/cpp/virglrenderer/server/virgl_server.c:125`.
- **Action**:
  - Updated managed callback signature to `long` and added bounded latch wait:
    - `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:73-105`
  - Added async prefetch during startup to reduce future blocking:
    - `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:49`
    - `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:161-174`

### 4) Hot-path Timber logging in VirGL

- **Classification**: proven hot-path issue
- **Evidence**:
  - Prior code logged `Timber.d(...)` around per-request `handleRequest(...)` and per-flush callback path.
- **Action**:
  - Removed per-request/per-flush debug logs from:
    - `handleRequest(...)` (`app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:121-133`)
    - `flushFrontbuffer(...)` (`app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java:136-159`)
  - Kept startup/shutdown and warning diagnostics only.

### 5) Weak or unproven claims

- **Generic global-ref lifecycle concerns**: **unproven / defer**
  - In reviewed hotspots, JNI object usage remains within active JNI calls. No new cross-call stored callback refs were introduced in this pass.
  - `virgl_server.c` uses `jni_info.obj` per JNI entry (`handleNewConnection`, `handleRequest`) and calls back during active call flow.
- **`GetDirectBufferAddress` optimization target**: **unproven / defer**
  - `Drawable` JNI paths already use direct buffers (`app/src/main/cpp/winlator/drawable.c:79-101,132-160,189,243-246`) with no extra copy layer to remove at JNI boundary.
- **Large protocol migration to native**: **unproven / defer**
  - No measurement in this pass justifies moving X11 or full VirGL protocol ownership across boundary.

## Instrumentation Added

### Native (XConnector)

- Added aggregated counters/timers in `app/src/main/cpp/winlator/xconnector_epoll.c:39-54` for:
  - epoll invocations, total events, callback count
  - ancillary recv/send counts and duration
  - wait-for-read count and duration
  - JNI lookup cache miss counts
- Added native snapshot API:
  - `Java_com_winlator_xconnector_XConnectorEpoll_getNativePerfStats(...)`
  - `app/src/main/cpp/winlator/xconnector_epoll.c:418-466`
- Exposed API on Java side:
  - `app/src/main/java/com/winlator/xconnector/XConnectorEpoll.java:41`
- Added debug-time dump hook at connector stop:
  - `app/src/main/java/com/winlator/xconnector/XConnectorEpoll.java:121-123`

### Managed (VirGL)

- Added sampled timing and frequency counters in `VirGLRendererComponent`:
  - request call count and sampled duration (`:121-133`)
  - flush callback count and sampled duration (`:136-159`)
  - shared EGL wait count and duration (`:102-104`)
  - aggregated dump/reset helper (`:176-204`)
- Added debug-time dump at component stop (`:62-64`).

## Measurements Gathered in This Pass

## Build/validation measurements

- `./gradlew :app:compileDebugJavaWithJavac :app:externalNativeBuildDebug` succeeded (19s, 16 actionable tasks).

## Evidence checks (post-change)

- JNI lookup now centralized in cache helper and no longer repeated in per-event epoll loop body.
- Hot per-request Timber logs removed in VirGL request/flush paths.
- `getSharedEGLContext` JNI signature aligned between native and managed:
  - native expects `()J` in `virgl_server.c:125`
  - managed method is `private long getSharedEGLContext()` in `VirGLRendererComponent.java:73`

## Runtime measurements

- Runtime collection hooks are now present, but no on-device render session was executed in this environment.
- Expected runtime log outputs are emitted on component/connector stop in debug builds.

## Implemented P0 Fixes

- **P0-A (cache JNI lookup state in hot paths)**: done in `xconnector_epoll.c`.
- **P0-B (remove/compile-gate hot native logs)**: done for epoll hot path in `xconnector_epoll.c`.
- **P0-C (remove hot managed logging)**: done for VirGL request/flush path in `VirGLRendererComponent.java`.
- **P0-D (reduce blocking EGL handoff safely)**: done via async prefetch + bounded wait with timing instrumentation in `VirGLRendererComponent.java`.

## Rejected or Deferred Audit Claims

- **Global ref lifecycle rewrite**: deferred; current reviewed paths do not show concrete cross-thread retention bug requiring broad rewrite.
- **`GetDirectBufferAddress` as optimization target**: deferred; direct buffer usage is already the intended fast path and not a measured bottleneck in this pass.
- **Large boundary migrations (X11/VirGL protocol)**: rejected for this pass; no measurement evidence collected here to justify subsystem rewrite risk.
- **Reflection-based ByteBuffer address mutation**: rejected; brittle and out-of-scope.

## Remaining High-Value Next Steps

1. Run a debug device session and capture native/managed instrumentation output for at least:
   - idle shell
   - menu navigation
   - active 3D frame rendering
2. Use captured metrics to rank whether next work is:
   - further callback frequency reduction in VirGL flush path, or
   - additional epoll boundary tightening.
3. If shared EGL wait still shows meaningful latency tails, split dedicated refactor ticket for deterministic context lifecycle ownership.

## Documentation Impact

Documentation Impact: Added follow-up execution report and remediation backlog artifacts for JNI boundary hardening.
