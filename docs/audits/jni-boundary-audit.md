# JNI Boundary Audit

## 1. Executive Summary

### Overall Assessment
The GameNative codebase makes moderate use of the JNI boundary, primarily for:
- X11 server networking (XConnectorEpoll - hot path)
- GPU rendering operations (Drawable, GPUImage)
- Vulkan/EGL interop (VirGLRendererComponent, VortekRendererComponent)
- Shared memory handling (SysVSharedMemory)

**Key Finding**: The JNI boundary is well-designed for the X11 networking layer, using direct ByteBuffers efficiently. However, there are several significant issues that affect performance, correctness, and maintainability.

### Top 5 Issues

1. **HP-1: Uncached JNI Method Lookups in Hot Path** - `xconnector_epoll.c:135-139` performs `GetObjectClass` + `GetMethodID` on every epoll iteration, instead of caching method IDs once.

2. **HP-2: Debug Logging in Tight Loops** - Heavy use of `printf` (remapped to `__android_log_print`) in epoll loop (`xconnector_epoll.c:145,199,236,275`) and GPU operations.

3. **HP-3: Synchronous Callback + Thread Wait Pattern** - `VirGLRendererComponent.getSharedEGLContext()` (lines 58-79) blocks the native thread while the UI thread does work, creating potential deadlock and latency issues.

4. **HP-4: ByteBuffer Allocation per Request** - `GPUImage.lockHardwareBuffer` returns a new `ByteBuffer` wrapper on every call rather than caching/mirroring the native buffer.

5. **AR-1: No Global Reference Management for Callbacks** - The code uses `jobject` callbacks without explicit `NewGlobalRef`/`DeleteGlobalRef`, relying on implicit behavior that may cause reference leaks in edge cases.

### Top 5 Recommended Actions

1. Cache JNI method IDs at connection initialization (P0)
2. Remove or guard all debug printf statements in hot paths (P0)
3. Replace blocking `getSharedEGLContext()` pattern with async callback or polling (P0)
4. Add JNI boundary call counters for profiling (P1)
5. Audit and fix callback reference lifecycle (P1)

### Quick Statement on JNI Overhead Impact
**JNI overhead is likely a contributing factor to performance issues, but it is not the primary bottleneck.** The critical hot paths (X11 event processing, VirGL rendering) already use direct ByteBuffers which is correct. The main issues are:
- Debug logging overhead (fixable)
- Method lookup caching (medium impact)
- Thread synchronization patterns (architectural issue)

The rendering via Drawable and GPUImage is already as efficient as possible given the current architecture (direct buffers, no copying).

---

## 2. Inventory of JNI Boundaries

### Native Library Loading

| Managed File | Library | Purpose |
|-------------|---------|---------|
| `com/winlator/xserver/Drawable.java` | `winlator_11` | X11 drawable operations |
| `com/winlator/xconnector/XConnectorEpoll.java` | `winlator` | X11 socket/epoll |
| `com/winlator/xconnector/ClientSocket.java` | `winlator` | Socket I/O |
| `com/winlator/core/GPUInformation.java` | `extras` | Vulkan queries |
| `com/winlator/core/GPUHelper.java` | `winlator_11` | Vulkan device info |
| `com/winlator/renderer/GPUImage.java` | `extras` | Hardware buffer/GPUImage |
| `com/winlator/sysvshm/SysVSharedMemory.java` | `winlator` | Shared memory |
| `com/winlator/xenvironment/components/VirGLRendererComponent.java` | `virglrenderer` | VirGL rendering |
| `com/winlator/xenvironment/components/VortekRendererComponent.java` | `vortekrenderer` | Vulkan rendering |

### Managed -> Native Entry Points

| Managed File | Native Symbol | Purpose | Frequency |
|-------------|---------------|---------|----------|
| `XConnectorEpoll.java` | `createAFUnixSocket` | Create Unix socket | Session |
| `XConnectorEpoll.java` | `createEpollFd` | Create epoll FD | Session |
| `XConnectorEpoll.java` | `doEpollIndefinitely` | **Main event loop** | **Per-frame/event** |
| `XConnectorEpoll.java` | `addFdToEpoll` | Add FD to epoll | Session |
| `XConnectorEpoll.java` | `removeFdFromEpoll` | Remove FD from epoll | Session |
| `XConnectorEpoll.java` | `createEventFd` | Create eventfd | Session |
| `XConnectorEpoll.java` | `waitForSocketRead` | Poll single socket | Per-client |
| `ClientSocket.java` | `read` | Socket read | **Per-frame** |
| `ClientSocket.java` | `write` | Socket write | **Per-frame** |
| `ClientSocket.java` | `recvAncillaryMsg` | Receive FD via SCM_RIGHTS | Event |
| `ClientSocket.java` | `sendAncillaryMsg` | Send FD via SCM_RIGHTS | Event |
| `Drawable.java` | `drawBitmap` | Draw bitmap to buffer | X11 request |
| `Drawable.java` | `copyArea` | Copy image region | X11 request |
| `Drawable.java` | `copyAreaOp` | Copy with raster op | X11 request |
| `Drawable.java` | `fillRect` | Fill rectangle | X11 request |
| `Drawable.java` | `drawLine` | Draw line | X11 request |
| `Drawable.java` | `drawAlphaMaskedBitmap` | Draw alpha bitmap | X11 request |
| `Drawable.java` | `fromBitmap` | Convert Bitmap to buffer | Init |
| `Pixmap.java` | `toBitmap` | Convert buffer to Bitmap | X11 request |
| `VirGLRendererComponent.java` | `handleNewConnection` | Handle new VirGL client | Session |
| `VirGLRendererComponent.java` | `handleRequest` | **Process VirGL protocol** | **Per-frame** |
| `VirGLRendererComponent.java` | `getCurrentEGLContextPtr` | Get EGL context | Session |
| `VirGLRendererComponent.java` | `destroyClient` | Destroy VirGL client | Session |
| `VirGLRendererComponent.java` | `destroyRenderer` | Destroy VirGL renderer | Session |
| `GPUImage.java` | `hardwareBufferFromSocket` | Create from socket | Init |
| `GPUImage.java` | `createHardwareBuffer` | Create AHardwareBuffer | Init |
| `GPUImage.java` | `destroyHardwareBuffer` | Destroy AHardwareBuffer | Cleanup |
| `GPUImage.java` | `lockHardwareBuffer` | Lock for CPU access | **Per-frame** |
| `GPUImage.java` | `createImageKHR` | Create EGL image | Init |
| `GPUImage.java` | `destroyImageKHR` | Destroy EGL image | Cleanup |
| `SysVSharedMemory.java` | `createMemoryFd` | Create shared memory FD | Session |
| `SysVSharedMemory.java` | `ashmemCreateRegion` | Create ashmem region | Session |
| `SysVSharedMemory.java` | `mapSHMSegment` | Map shared memory | Session |
| `SysVSharedMemory.java` | `unmapSHMSegment` | Unmap shared memory | Session |
| `GPUInformation.java` | `getVulkanVersion` | Query Vulkan version | Init |
| `GPUInformation.java` | `getRenderer` | Query renderer string | Init |
| `GPUInformation.java` | `enumerateExtensions` | Query Vulkan extensions | Init |
| `GPUInformation.java` | `getVendorID` | Query vendor ID | Init |
| `GPUHelper.java` | `vkGetApiVersion` | Get Vulkan API version | Init |
| `GPUHelper.java` | `vkGetDeviceExtensions` | Get device extensions | Init |
| `PatchElf.java` | `createElfObject` | Create ELF parser object | Init |
| `PatchElf.java` | `destroyElfObject` | Destroy ELF parser object | Cleanup |
| `PatchElf.java` | `isChanged` | Check if ELF was modified | Init |
| `PatchElf.java` | `getInterpreter` | Get interpreter path | Init |
| `PatchElf.java` | `setInterpreter` | Set interpreter path | Init |
| `PatchElf.java` | `getOsAbi` | Get OS ABI | Init |
| `PatchElf.java` | `replaceOsAbi` | Replace OS ABI | Init |
| `PatchElf.java` | `getSoName` | Get soname | Init |
| `PatchElf.java` | `replaceSoName` | Replace soname | Init |
| `PatchElf.java` | `getRPath` | Get rpath | Init |
| `PatchElf.java` | `addRPath` | Add rpath | Init |
| `PatchElf.java` | `removeRPath` | Remove rpath | Init |
| `PatchElf.java` | `getNeeded` | Get needed libs | Init |
| `PatchElf.java` | `addNeeded` | Add needed lib | Init |
| `PatchElf.java` | `removeNeeded` | Remove needed lib | Init |
| `VortekRendererComponent.java` | `createVkContext` | Create Vulkan context | Session |
| `VortekRendererComponent.java` | `destroyVkContext` | Destroy Vulkan context | Session |
| `VortekRendererComponent.java` | `handleExtraDataRequest` | Handle Vulkan extra data | Per-request |
| `VortekRendererComponent.java` | `initVulkanWrapper` | Initialize Vulkan wrapper | Init |

### Native -> Managed Callbacks (@Keep annotated)

| Managed File | Method | Called From | Purpose | Frequency |
|-------------|--------|-------------|---------|----------|
| `XConnectorEpoll.java` | `handleNewConnection(int fd)` | `xconnector_epoll.c:151,154` | New client connection | Session/Event |
| `XConnectorEpoll.java` | `handleExistingConnection(int fd)` | `xconnector_epoll.c:158,238,294` | Data available on socket | **Per-frame** |
| `ClientSocket.java` | `addAncillaryFd(int fd)` | `xconnector_epoll.c:238` | FD received via SCM_RIGHTS | Event |
| `VirGLRendererComponent.java` | `killConnection(int fd)` | virgl_server.c (internal) | Kill stale connection | Rare |
| `VirGLRendererComponent.java` | `getSharedEGLContext()` | virgl_server.c (via GetMethodID) | Get EGL context | Session |
| `VirGLRendererComponent.java` | `flushFrontbuffer(int, int)` | virgl_server.c (internal) | Frontbuffer flush | **Per-frame** |
| `VortekRendererComponent.java` | `getWindowWidth(int)` | vulkan.c | Get window width | Frame |
| `VortekRendererComponent.java` | `getWindowHeight(int)` | vulkan.c | Get window height | Frame |
| `VortekRendererComponent.java` | `getWindowHardwareBuffer(int)` | vulkan.c | Get buffer handle | Frame |
| `VortekRendererComponent.java` | `updateWindowContent(int)` | vulkan.c | Update window | Frame |
| `GPUImage.java` | `setStride(short)` | `gpu_image.c:157,163,173` | Report stride after lock | Init |

---

## 3. Hot Path Findings

### HP-1: Uncached JNI Method Lookups in Epoll Loop

**ID**: HP-1  
**Title**: Repeated GetObjectClass/GetMethodID in Tight Loop  
**Affected Files**: 
- `app/src/main/cpp/winlator/xconnector_epoll.c` lines 135-139, 232-233, 292-293

**Why Hot**: `doEpollIndefinitely` runs continuously while the X server is active. Every iteration that processes events calls `GetObjectClass(env, obj)` and `GetMethodID(env, cls, "handleNewConnection", "(I)V")` - even though the class and method never change.

**Evidence**:
```c
// xconnector_epoll.c:130-161
JNIEXPORT jboolean JNICALL
Java_com_winlator_xconnector_XConnectorEpoll_doEpollIndefinitely(JNIEnv *env, jobject obj,
                                                               jint epollFd, jint serverFd,
                                                               jboolean addClientToEpoll) {
    jclass cls = (*env)->GetObjectClass(env, obj);          // CALLED EVERY ITERATION
    jmethodID handleNewConnection =                         // CALLED EVERY ITERATION
            (*env)->GetMethodID(env, cls, "handleNewConnection", "(I)V");
    // ... epoll_wait and callback loop
}
```

Same pattern in `waitForSocketRead` (lines 292-293) and `recvAncillaryMsg` (lines 232-233).

**Cost Type**: `lookup`  
**Recommendation**: Cache method IDs after first lookup, store in static variables or struct. Add init function to cache all method IDs once at connection setup.

**Implementation Complexity**: S  
**Expected Impact**: Medium  
**Confidence**: High  

---

### HP-2: Debug Logging in Hot Paths

**ID**: HP-2  
**Title**: Excessive printf/debug Logging in Tight Loops  
**Affected Files**:
- `app/src/main/cpp/winlator/xconnector_epoll.c` - multiple printf in epoll loop
- `app/src/main/cpp/winlator/drawable.c` - error path printf
- `app/src/main/cpp/extras/gpu_image.c` - error path printf
- Managed side: Heavy Timber usage in hot paths (e.g., `VirGLRendererComponent.java` lines 97-101)

**Why Hot**: 
1. `xconnector_epoll.c:145` - `printf("xconnector_epoll.c accept %d", clientFd)` inside accept loop
2. `xconnector_epoll.c:199` - `printf("xconnector_epoll.c eventfd %d", fd)` at eventfd creation
3. `xconnector_epoll.c:236` - `printf("xconnector_epoll.c CMSG_DATA %d", ancillaryFd)` in FD passing
4. `xconnector_epoll.c:275` - `printf("xconnector_epoll.c sendmsg size %d", size)` after every send

These execute on every X11 event cycle (thousands per second during active gaming).

**Evidence**: Grep shows 537 printf uses in cpp/ directory. Many are in render hot paths.

**Cost Type**: `logging`  
**Recommendation**: 
1. Remove all non-error printf from hot paths
2. Add compile-time debug flag (NDEBUG) to guard printf
3. Replace Timber.d in VirGLRendererComponent with Timber.v or remove entirely

**Implementation Complexity**: S  
**Expected Impact**: High (logging overhead can be significant)  
**Confidence**: High  

---

### HP-3: Blocking Callback Pattern in VirGL

**ID**: HP-3  
**Title**: Synchronous Thread Wait in getSharedEGLContext  
**Affected Files**:
- `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java` lines 58-79

**Why Hot**: The native VirGL server calls `getSharedEGLContext()` callback which blocks the native thread while the UI thread initializes EGL. This creates:
- Potential deadlock if UI thread is busy
- Unpredictable latency spikes
- Cannot be traced by standard JNI instrumentation

**Evidence**:
```kotlin
// VirGLRendererComponent.java:58-79
@Keep
private void getSharedEGLContext() {
    Timber.tag("VirGLRendererComponent").d("Calling getSharedEGLContext")
    if (sharedEGLContextPtr != 0) return
    val thread = Thread.currentThread()
    try {
        val renderer = xServer.getRenderer()
        renderer.xServerView.queueEvent() {   // Queue work to UI thread
            sharedEGLContextPtr = getCurrentEGLContextPtr()  // Native call on UI thread
            synchronized(thread) { thread.notify() }
        }
        synchronized(thread) { thread.wait() }   // BLOCK NATIVE THREAD
    } catch (e: Exception) { return }
}
```

**Cost Type**: `threading` + `blocking`  
**Recommendation**: 
- Change to callback-based async pattern: native code provides a callback function pointer, managed side calls it when ready
- Or: Pre-initialize EGL context during component start, pass via stored handle

**Implementation Complexity**: M  
**Expected Impact**: High (latency/ deadlock risk)  
**Confidence**: High  

---

### HP-4: ByteBuffer Allocation Per Frame

**ID**: HP-4  
**Title**: New ByteBuffer Created on Every lockHardwareBuffer Call  
**Affected Files**:
- `app/src/main/cpp/extras/gpu_image.c` lines 143-183
- `app/src/main/cpp/winlator/gpu_image.c` (same pattern)

**Why Hot**: `lockHardwareBuffer` creates a new `NewDirectByteBuffer` on each call:
```c
// gpu_image.c:175-180
virtualData = (unsigned char*)(*env)->GetDirectBufferAddress(env, buffer);
*env)->CallVoidMethod(env, obj, setStrideMethod, stride);
return (*env)->NewDirectByteBuffer(env, lockedBuffer, bufferSize);
```

This happens every time the GPU needs to read/write pixel data.

**Cost Type**: `allocation` + `copying`  
**Recommendation**: 
- The stride callback is the only dynamic part; buffer address/capacity are constant
- Pre-allocate a single ByteBuffer wrapper and update its address via reflection or expose native-side buffer directly

**Implementation Complexity**: M  
**Expected Impact**: Medium  
**Confidence**: Medium  

---

### HP-5: Direct ByteBuffer Address Lookup Per Operation

**ID**: HP-5  
**Title**: GetDirectBufferAddress Called for Every Drawable Operation  
**Affected Files**:
- `app/src/main/cpp/winlator/drawable.c` - all functions (lines 79, 100, 132, 160)

**Why Hot**: For every X11 drawing operation (copyArea, fillRect, drawLine), the native code calls `GetDirectBufferAddress`:
```c
// drawable.c:94-105
JNIEXPORT void JNICALL
Java_com_winlator_xserver_Drawable_copyArea(...) {
    uint8_t *srcDataAddr = (*env)->GetDirectBufferAddress(env, srcData);
    uint8_t *dstDataAddr = (*env)->GetDirectBufferAddress(env, dstData);
    // ... per-pixel operation
}
```

**Evidence**: Called in all Drawable operations (copyArea, copyAreaOp, drawLine, fillRect, etc.)

**Cost Type**: `lookup`  
**Recommendation**: This is actually well-optimized - direct ByteBuffers are the right approach. The issue is more about whether the per-pixel operations should be in native at all. Consider batching multiple draw operations or moving entire Drawable manipulation to native.

**Implementation Complexity**: L (this is already well-optimized)  
**Expected Impact**: Low (direct buffers are efficient)  
**Confidence**: High  

---

### HP-6: X11 Request Dispatch via JNI

**ID**: HP-6  
**Title**: handleRequest JNI Call Per X11 Request  
**Affected Files**:
- `app/src/main/java/com/winlator/xenvironment/components/VirGLRendererComponent.java` line 100
- `app/src/main/cpp/virglrenderer/server/virgl_server.c` line 135

**Why Hot**: Every VirGL protocol message (thousands per frame during rendering) triggers:
1. Native: `virgl_server_handle_request()` 
2. Native callback to Java: `flushFrontbuffer(int, int)` - per framebuffer swap
3. Native callback to Java: `getSharedEGLContext()` - once per connection

**Evidence**: Line 132-136 in virgl_server.c calls into Java for each request, which then calls back into native for rendering.

**Cost Type**: `crossing count`  
**Recommendation**: 
- Batch multiple rendering commands before callback
- Use native-side command buffer for VirGL operations
- Consider moving entire VirGL protocol handling to native

**Implementation Complexity**: L  
**Expected Impact**: High  
**Confidence**: Medium  

---

### HP-7: Unreleased String Resources in wine_registry_editor.c

**ID**: HP-7  
**Title**: GetStringUTFChars/GetStringChars Without Guaranteed Release  
**Affected Files**:
- `app/src/main/cpp/winlator/wine_registry_editor.c` lines 27-33

**Why Hot**: This function may be called during container setup for each registry key lookup. While there is code to release strings, the error path may not properly release them.

**Evidence**:
```c
const char   *path8  = (*env)->GetStringUTFChars(env, filePath, NULL);
const jchar  *key16  = (*env)->GetStringChars(env, key, NULL);
// ... if early return happens, these may leak
(*env)->ReleaseStringChars(env, key, key16);  // only one release path
// path8 may not be released in all code paths
```

**Cost Type**: `allocation` + `memory leak`  
**Recommendation**: Ensure all code paths release string resources properly, use goto labels for cleanup.

**Implementation Complexity**: S  
**Expected Impact**: Low (only during container setup)  
**Confidence**: Medium  

---

### HP-8: GetStringChars (Unicode) Overhead

**ID**: HP-8  
**Title**: Using GetStringChars Instead of GetStringUTFChars  
**Affected Files**:
- `app/src/main/cpp/winlator/wine_registry_editor.c` line 28

**Why Hot**: `GetStringChars` returns jchar* (UTF-16) which then needs conversion to UTF-8 for file operations. `GetStringUTFChars` would avoid this conversion.

**Evidence**: Line 28 uses `GetStringChars`, then lines 30-32 manually copy to wchar_t.

**Cost Type**: `copying`  
**Recommendation**: Use GetStringUTFChars directly if the native code only needs UTF-8.

**Implementation Complexity**: S  
**Expected Impact**: Low (cold path)  
**Confidence**: High  

---

## 4. Correctness / Reliability Risks at the Boundary

### AR-1: No Global Reference Management for Callbacks

**Risk**: Local reference lifetime uncertainty  
**Files**: 
- `xconnector_epoll.c` - callbacks (handleNewConnection, handleExistingConnection)
- `gpu_image.c` - setStride callback

**Details**: The code stores `jobject obj` from JNI and uses it in callbacks. In standard JNI, local references are valid only until the JNI call returns. However, in long-running native loops that call back to Java, these references may become invalid.

**Evidence**: No `NewGlobalRef`/`DeleteGlobalRef` in codebase. Code relies on implicit behavior.

**Severity**: Medium (likely works in practice because JVM doesn't collect the calling object, but undefined behavior)

---

### AR-2: Thread Safety in XConnectorEpoll Callbacks

**Risk**: Race conditions between native epoll thread and Java callbacks  
**Files**: `XConnectorEpoll.java`, `xconnector_epoll.c`

**Details**: 
- Native epoll thread owns the JNIEnv
- Callbacks run on native thread
- If Java side modifies shared state during callback, potential race
- No explicit synchronization around `connectedClients` map access

**Evidence**: 
- `XConnectorEpoll.java:164` - `requestHandler.handleRequest(client)` called during callback
- `connectedClients` is a `ConcurrentHashMap` but individual Client modifications may not be atomic

**Severity**: Medium

---

### AR-3: Exception Handling in JNI

**Risk**: Unchecked exceptions across boundary  
**Files**: All native->Java callbacks

**Details**: No `ExceptionCheck()` or `ExceptionOccurred()` calls found. If a callback throws an exception, it may propagate incorrectly.

**Evidence**: grep shows no exception handling in native C files.

**Severity**: Low

---

### AR-4: String Marshaling in Init Path

**Risk**: Temporary jstring memory not released  
**Files**: 
- `extras/vulkan.c` - getVulkanVersion, getRenderer, enumerateExtensions
- `gpu_helper.c` - vkGetDeviceExtensions

**Details**: Functions like `getVulkanVersion` call `GetStringUTFChars` but may not properly release if exceptions occur mid-function.

```c
// extras/vulkan.c:222-272
JNIEXPORT jstring JNICALL
Java_com_winlator_core_GPUInformation_getVulkanVersion(JNIEnv *env, jclass obj, jstring driverName, jobject context) {
    const char *unknown = "Unknown";
    // driverName is obtained as UTF but not explicitly released in all code paths
```

**Severity**: Low (minor, happens only at init)

---

### AR-5: Buffer Address Validity

**Risk**: ByteBuffer address becoming invalid  
**Files**: `drawable.c`, `gpu_image.c`

**Details**: `GetDirectBufferAddress` returns a pointer that becomes invalid if the ByteBuffer is garbage collected. The code relies on the managed side keeping buffers alive.

**Evidence**: No `NewGlobalRef` held for ByteBuffers. Relies on GC root from local reference.

**Severity**: Low (works in practice because buffers are scoped to Drawable lifetime)

---

## 5. Architectural Refactor Opportunities

### 5.1 X11 Event Processing

**Current Boundary**:
- Native: epoll_wait, socket accept/read
- Managed: XConnectorEpoll runs loop, dispatches to XClientRequestHandler
- Per-X11-request JNI crossing: handleRequest()

**Problems**:
1. Every X11 request causes managed->native->managed round-trip
2. Heavy object allocation in request path (XInputStream, XOutputStream)
3. No batching of requests

**Recommendation**:
- **Keep boundary as-is for now** - X11 protocol parsing is complex, keeping it in Kotlin/Java is appropriate for maintainability
- Batch multiple X11 requests where possible
- Cache method IDs in native (HP-1 fix)

**Rationale**: X11 protocol is extensible, needs managed-side handling for extensions. Moving to native would require significant complexity.

---

### 5.2 Rendering Pipeline (VirGL)

**Current Boundary**:
- Native: VirGL protocol handling, GL command processing
- Managed: flushFrontbuffer callback, EGL context setup
- Per-frame JNI crossing: handleRequest + flushFrontbuffer

**Problems**:
1. Blocking getSharedEGLContext pattern
2. Per-frame callbacks for frontbuffer flush
3. Heavy Timber logging in hot path

**Recommendation**:
- **Move EGL context initialization to session start**, pass as handle
- Replace flushFrontbuffer callback with **polling/shared state**:
  - Native writes frame ready flag to shared memory/direct buffer
  - Managed polls flag in render loop instead of callback
- Remove Timber.d from VirGLRendererComponent hot paths

**Rationale**: Rendering is the most latency-critical path. Reducing JNI crossings here has highest impact.

---

### 5.3 GPU Image Handling

**Current Boundary**:
- Native: AHardwareBuffer creation, locking, EGL image
- Managed: GPUImage wrapper, texture binding

**Problems**:
1. ByteBuffer wrapper recreated each lock
2. Stride callback adds JNI overhead

**Recommendation**:
- Keep boundary but **cache ByteBuffer wrapper**
- Pre-compute stride during buffer creation, store in native struct

**Rationale**: Already using direct buffers correctly. Main issue is allocation/batching.

---

### 5.4 Audio (ALSA)

**Current Boundary**:
- Fully managed (ALSAClient.java)
- Uses SysVSharedMemory for shared buffer with native side
- No JNI in hot path (AudioTrack is native Android API)

**Assessment**: **No changes needed**. Audio path is already optimal - shared memory for buffer, AudioTrack for output.

---

### 5.5 Input Handling

**Current Boundary**:
- Fully managed (WinHandler, Keyboard)
- No JNI crossing

**Assessment**: **No changes needed**. Input goes through Android event system to managed code, then to XServer.

---

## 6. Prioritized Remediation Plan

### P0: Obvious Hot-Path Fixes / Correctness Hazards

| Item | Description | Files | Rationale | Risk |
|------|-------------|-------|-----------|------|
| P0-1 | Remove/guard printf in epoll loop | `xconnector_epoll.c:145,199,236,275` | Debug logging in tight loop | Low - just removes logging |
| P0-2 | Cache JNI method IDs in XConnectorEpoll | `xconnector_epoll.c:135-139,232-233,292-293` | Repeated lookups per event | Low - standard caching pattern |
| P0-3 | Fix blocking getSharedEGLContext | `VirGLRendererComponent.java:58-79` | Deadlock/latency risk | Medium - changes async pattern |
| P0-4 | Remove Timber.d from hot paths | `VirGLRendererComponent.java` lines 97-101 | Logging overhead per frame | Low - just remove logging |

### P1: Batching / Caching / Boundary Redesign

| Item | Description | Files | Rationale | Risk |
|------|-------------|-------|-----------|------|
| P1-1 | Cache ByteBuffer wrapper for GPUImage | `gpu_image.c` | Allocation per frame | Low - buffer reuse |
| P1-2 | Add JNI call counters | All JNI boundaries | Enable profiling | Low - instrumentation |
| P1-3 | Batch VirGL flushFrontbuffer | `virgl_server.c`, `VirGLRendererComponent.java` | Reduce callback frequency | Medium - changes sync semantics |
| P1-4 | Audit callback reference lifecycle | All @Keep methods | Prevent ref leaks | Medium - requires analysis |

### P2: Larger Refactors

| Item | Description | Files | Rationale | Risk |
|------|-------------|-------|-----------|------|
| P2-1 | Move VirGL protocol to native side | VirGL components | Reduce JNI crossings | High - major rewrite |
| P2-2 | Implement shared-state rendering | VirGL components | Replace callbacks with polling | High - architecture change |
| P2-3 | Add NDEBUG guard to native printf | All cpp files | Production performance | Low - build change |

---

## 7. Suggested Instrumentation

### 7.1 JNI Call Counters

Add atomic counters at each JNI boundary:

**File**: Add to `XConnectorEpoll.java`
```kotlin
object JNICounters {
    var epollIterations: Long = 0
    var handleRequestCalls: Long = 0
    var socketReadCalls: Long = 0
    var socketWriteCalls: Long = 0
}
```

**Native side**: Use `android_atomic_inc` or similar to increment from native code.

### 7.2 Timing Instrumentation

Add timing around JNI calls in hot paths:

```kotlin
// VirGLRendererComponent.handleRequest
val start = System.nanoTime()
handleRequest(clientPtr)  // JNI call
val duration = System.nanoTime() - start
if (duration > 100_000) { // > 100us
    Timber.w("handleRequest took ${duration/1000}us")
}
```

### 7.3 Memory/Allocation Tracking

Monitor ByteBuffer allocation rate:
- Track `lockHardwareBuffer` call frequency
- Track `copyArea`/`fillRect` call frequency from DrawRequests

### 7.4 Profiling Hooks

Add `System.nanoTime()` before/after:
- `doEpollIndefinitely` - measure epoll loop iteration time
- `handleRequest` in VirGLRendererComponent - measure VirGL processing time
- `flushFrontbuffer` - measure frame flush time

---

## 8. Appendix: Raw JNI Inventory

### Native Libraries Loaded (System.loadLibrary)

| File | Library | Line |
|------|---------|------|
| `Drawable.java` | `winlator_11` | 40 |
| `XConnectorEpoll.java` | `winlator` | 47 |
| `ClientSocket.java` | `winlator` | 14 |
| `GPUInformation.java` | `extras` | 28 |
| `GPUHelper.java` | `winlator_11` | 17 |
| `GPUImage.java` | `extras` | 15 |
| `SysVSharedMemory.java` | `winlator` | 18 |
| `VirGLRendererComponent.java` | `virglrenderer` | 27 |
| `VortekRendererComponent.java` | `vortekrenderer` | 41 |
| `PatchElf.java` | `winlator` | 9 |

### JNI Function Usage Summary

| Function | Files Using | Usage Context |
|----------|-------------|---------------|
| `GetDirectBufferAddress` | drawable.c, xconnector_epoll.c, gpu_image.c, sysvshared_memory.c | All buffer operations |
| `NewDirectByteBuffer` | gpu_image.c, sysvshared_memory.c | Buffer wrapper creation |
| `GetMethodID` | xconnector_epoll.c, gpu_image.c | Callback invocation |
| `CallVoidMethod` | xconnector_epoll.c, gpu_image.c | Native->Managed callbacks |
| `GetObjectClass` | xconnector_epoll.c, gpu_image.c | Method lookup |
| `FindClass` | gpu_helper.c, vulkan.c | Class lookup |
| `NewStringUTF` | gpu_helper.c, vulkan.c | Return string creation |
| `GetStringUTFChars` | xconnector_epoll.c, sysvshared_memory.c, vulkan.c | String parameter handling |
| `ReleaseStringUTFChars` | xconnector_epoll.c, sysvshared_memory.c, vulkan.c | String cleanup |
| `NewObjectArray` | gpu_helper.c, vulkan.c | Return array creation |
| `SetObjectArrayElement` | gpu_helper.c, vulkan.c | Array population |
| `DeleteLocalRef` | gpu_helper.c | Reference cleanup |

### JNI Symbol to File Mapping

| Native Symbol | Source File | Lines |
|---------------|------------|-------|
| `Java_com_winlator_xconnector_XConnectorEpoll_createAFUnixSocket` | xconnector_epoll.c | 80-107 |
| `Java_com_winlator_xconnector_XConnectorEpoll_createEpollFd` | xconnector_epoll.c | 109-115 |
| `Java_com_winlator_xconnector_XConnectorEpoll_closeFd` | xconnector_epoll.c | 117-128 |
| `Java_com_winlator_xconnector_XConnectorEpoll_doEpollIndefinitely` | xconnector_epoll.c | 130-162 |
| `Java_com_winlator_xconnector_XConnectorEpoll_addFdToEpoll` | xconnector_epoll.c | 164-173 |
| `Java_com_winlator_xconnector_XConnectorEpoll_removeFdFromEpoll` | xconnector_epoll.c | 175-179 |
| `Java_com_winlator_xconnector_XConnectorEpoll_createEventFd` | xconnector_epoll.c | 196-202 |
| `Java_com_winlator_xconnector_XConnectorEpoll_waitForSocketRead` | xconnector_epoll.c | 279-297 |
| `Java_com_winlator_xconnector_ClientSocket_read` | xconnector_epoll.c | 182-186 |
| `Java_com_winlator_xconnector_ClientSocket_write` | xconnector_epoll.c | 188-194 |
| `Java_com_winlator_xconnector_ClientSocket_recvAncillaryMsg` | xconnector_epoll.c | 204-245 |
| `Java_com_winlator_xconnector_ClientSocket_sendAncillaryMsg` | xconnector_epoll.c | 247-277 |
| `Java_com_winlator_xserver_Drawable_drawBitmap` | drawable.c | 75-92 |
| `Java_com_winlator_xserver_Drawable_copyArea` | drawable.c | 94-124 |
| `Java_com_winlator_xserver_Drawable_copyAreaOp` | drawable.c | 126-154 |
| `Java_com_winlator_xserver_Drawable_fillRect` | drawable.c | 156-183 |
| `Java_com_winlator_xserver_Drawable_drawLine` | drawable.c | 185-234 |
| `Java_com_winlator_xserver_Drawable_drawAlphaMaskedBitmap` | drawable.c | 236-259 |
| `Java_com_winlator_xserver_Drawable_fromBitmap` | drawable.c | 262-280 |
| `Java_com_winlator_xserver_Pixmap_toBitmap` | drawable.c | 282-313 |
| `Java_com_winlator_renderer_GPUImage_hardwareBufferFromSocket` | gpu_image.c | 91-108 |
| `Java_com_winlator_renderer_GPUImage_createHardwareBuffer` | gpu_image.c | 111-119 |
| `Java_com_winlator_renderer_GPUImage_createImageKHR` | gpu_image.c | 122-130 |
| `Java_com_winlator_renderer_GPUImage_destroyHardwareBuffer` | gpu_image.c | 133-140 |
| `Java_com_winlator_renderer_GPUImage_lockHardwareBuffer` | gpu_image.c | 143-183 |
| `Java_com_winlator_renderer_GPUImage_destroyImageKHR` | gpu_image.c | 186-193 |
| `Java_com_winlator_sysvshm_SysVSharedMemory_ashmemCreateRegion` | sysvshared_memory.c | 71-77 |
| `Java_com_winlator_sysvshm_SysVSharedMemory_mapSHMSegment` | sysvshared_memory.c | 79-84 |
| `Java_com_winlator_sysvshm_SysVSharedMemory_unmapSHMSegment` | sysvshared_memory.c | 86-91 |
| `Java_com_winlator_sysvshm_SysVSharedMemory_createMemoryFd` | sysvshared_memory.c | 93-113 |
| `Java_com_winlator_xenvironment_components_VirGLRendererComponent_handleNewConnection` | virgl_server.c | 118-129 |
| `Java_com_winlator_xenvironment_components_VirGLRendererComponent_handleRequest` | virgl_server.c | 131-137 |
| `Java_com_winlator_xenvironment_components_VirGLRendererComponent_destroyClient` | virgl_server.c | 138-143 |
| `Java_com_winlator_xenvironment_components_VirGLRendererComponent_destroyRenderer` | virgl_server.c | 144-149 |
| `Java_com_winlator_xenvironment_components_VirGLRendererComponent_getCurrentEGLContextPtr` | virgl_server_renderer.c | 592-599 |
| `Java_com_winlator_core_GPUInformation_getVulkanVersion` | vulkan.c | 222-272 |
| `Java_com_winlator_core_GPUInformation_getVendorID` | vulkan.c | 274-305 |
| `Java_com_winlator_core_GPUInformation_getRenderer` | vulkan.c | 307-356 |
| `Java_com_winlator_core_GPUInformation_enumerateExtensions` | vulkan.c | 358-428 |
| `Java_com_winlator_core_GPUHelper_vkGetApiVersion` | gpu_helper.c | 59-295 |
| `Java_com_winlator_core_GPUHelper_vkGetDeviceExtensions` | gpu_helper.c | 301-403 |

---

*End of Audit*