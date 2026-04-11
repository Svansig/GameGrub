# JNI Next-Step Recommendation

Date: 2026-04-11

## Recommended next implementation target

### Capture sustained virgl-active runtime metrics on a virgl-stable title/session

**Why this wins now**
- External-launch override propagation is now fixed and verified with runtime logs showing `Graphics Driver: virgl`.
- The measured virgl test title exited quickly, so `VirGL JNI perf` was emitted but remained near-zero/inactive at live dump points.
- The strongest remaining JNI unknowns are still VirGL `requests`/`flush`/`sharedEglWait` under sustained rendering.
- XConnector remains stable (cache misses zero after warmup, small ancillary per-call cost), so further epoll micro-work is still lower value.

**Likely files to change**
- No production-code change required if a virgl-stable title is already available.
- If additional measurement hooks are needed, likely touchpoints are:
  - `app/src/main/java/app/gamegrub/debug/JniStatsDumpReceiver.kt`
  - `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java`

**Success looks like**
- `Graphics Driver: virgl` confirmed in environment startup logs.
- `JniStatsDumpReceiver` emits non-zero in-session `VirGL JNI perf` request/flush counters.
- Derived rates (`calls/s`, sampled avg us) are available for both startup and active windows.
- Next optimization decision (flush callback reduction vs EGL lifecycle refactor) is made from measured numbers.

## Backup target

### Add a targeted virgl-session smoke scenario that stays alive long enough for JNI sampling

**Why this is the backup**
- Current selected title under virgl exits too quickly to populate live counters.
- A minimal stable scene/session path would unblock actionable virgl JNI sampling without broad refactors.

**Likely files to change**
- `app/src/main/java/app/gamegrub/ui/screen/HomeScreen.kt` (if adding a debug entry point)
- `app/src/main/java/app/gamegrub/ui/screen/xserver/XServerScreen.kt`
- `app/src/main/java/app/gamegrub/debug/JniStatsDumpReceiver.kt`

**Success looks like**
- Session remains alive for at least one 30s active-window virgl dump.
- `VirGL JNI perf` live dumps report non-zero request/flush values.
- Measurement run is repeatable across at least two launches.

## Why these beat the other options

- **Further VirGL flush callback reduction**: not justified until VirGL is actually measured
- **Additional epoll boundary tightening**: measured data says cache churn is already fixed and per-call ancillary JNI cost is small
- **Deterministic EGL context lifecycle refactor**: too early without a real VirGL session using current instrumentation
- **GPUImage / ByteBuffer work**: still unmeasured and not implicated by current device evidence
- **Global-ref lifecycle review**: still a correctness audit item, not a measured runtime bottleneck
- **Broader JNI boundary redesign**: explicitly not justified by current evidence

## Do NOT work on yet

- Full X11 boundary migration
- Full VirGL protocol migration to native
- Generic JNI cleanup passes
- `GetDirectBufferAddress` optimization work
- Reflection-based buffer hacks
- Broad callback/global-ref rewrites without a concrete failure or measured cost
