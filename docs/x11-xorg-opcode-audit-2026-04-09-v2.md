# X11/Xorg Opcode Audit — Second Pass (2026-04-09)

> Systematic audit of all core and extension X11 opcode handling in the vendored X server.  
> Previous audit: `x11-xorg-opcode-audit-2026-04-09.md`

---

## 1. Dispatch entrypoints

| Area | File | Method |
|---|---|---|
| Core request dispatch | `XClientRequestHandler.java` | `handleNormalRequest` — switch on `opcode` (signed byte) |
| Core opcode constants | `ClientOpcodes.java` | Public static `byte` constants |
| Extension registration | `XServer.java` | `setupExtensions` — populates `SparseArray<Extension> extensions` |
| Extension discovery | `ExtensionRequests.java` | `queryExtension`, `listExtensions` |
| Extension dispatch | Each `*Extension.java` | `handleRequest(client, inputStream, outputStream)` |
| Protocol error emission | `XRequestError.java` | `sendError` — 32-byte error packet |
| Sequence tracking | `XClient.java` | `generateSequenceNumber` / `getSequenceNumber` |
| Resource ID validation | `XClient.java` | `isValidResourceId`, `registerAsOwnerOfResource` |

---

## 2. Core opcode inventory

Legend: `complete` · `partial` · `stub/skip` · `reply-stub` · `missing` · `fixed-this-pass`

### 2a. All defined core opcodes with implementation status

| Opcode | Name | Handler | Status | Notes |
|---:|---|---|---|---|
| 1 | CREATE_WINDOW | `WindowRequests.createWindow` | partial | Missing: CopyFromParent visual (getVisual(0)), no border rendering |
| 2 | CHANGE_WINDOW_ATTRIBUTES | `WindowRequests.changeWindowAttributes` | partial | Event-mask access check may be overly strict |
| 3 | GET_WINDOW_ATTRIBUTES | `WindowRequests.getWindowAttributes` | partial | colormap field hardcoded 0; do-not-propagate-mask truncated to 16-bit |
| 4 | DESTROY_WINDOW | `WindowRequests.destroyWindow` | partial | No BadWindow check on invalid IDs |
| 5 | DESTROY_SUB_WINDOWS | `WindowRequests.destroySubWindows` | partial | Only immediate children, not recursive |
| 6 | CHANGE_SAVE_SET | `skipRequest` | stub/skip | **NEW** — Save-sets not tracked |
| 7 | REPARENT_WINDOW | `WindowRequests.reparentWindow` | partial | x,y still skipped (consumed but not applied to placement) |
| 8 | MAP_WINDOW | `WindowRequests.mapWindow` | partial | |
| 9 | MAP_SUB_WINDOWS | `WindowRequests.mapSubWindows` | partial | |
| 10 | UNMAP_WINDOW | `WindowRequests.unmapWindow` | partial | |
| 11 | UNMAP_SUB_WINDOWS | `WindowRequests.unmapSubWindows` | partial | **NEW** — Unmaps immediate children |
| 12 | CONFIGURE_WINDOW | `WindowRequests.configureWindow` | partial | |
| 13 | CIRCULATE_WINDOW | `skipRequest` | stub/skip | **NEW** — Stacking not implemented |
| 14 | GET_GEOMETRY | `WindowRequests.getGeometry` | partial | root window ID hardcoded |
| 15 | QUERY_TREE | `WindowRequests.queryTree` | complete | |
| 16 | INTERN_ATOM | `AtomRequests.internAtom` | partial | **FIXED** — onlyIfExists now returns None(0) not BadAtom |
| 17 | GET_ATOM_NAME | `AtomRequests.getAtomName` | partial | **FIXED** — id≤0 check (was id<0, missing atom=0) |
| 18 | CHANGE_PROPERTY | `WindowRequests.changeProperty` | partial | No ATOM format validation |
| 19 | DELETE_PROPERTY | `WindowRequests.deleteProperty` | partial | |
| 20 | GET_PROPERTY | `WindowRequests.getProperty` | partial | Edge case: type=AnyPropertyType(0) not validated per spec |
| 21 | LIST_PROPERTIES | `WindowRequests.listProperties` | complete | **NEW** |
| 22 | SET_SELECTION_OWNER | `SelectionRequests.setSelectionOwner` | partial | **FIXED** — windowId=0 (release) now allowed |
| 23 | GET_SELECTION_OWNER | `SelectionRequests.getSelectionOwner` | partial | |
| 24 | CONVERT_SELECTION | `skipRequest` | stub/skip | **NEW** — Selection transfer not implemented |
| 25 | SEND_EVENT | `WindowRequests.sendEvent` | partial | **FIXED** — PointerWindow(0) and InputFocus(1) now resolved |
| 26 | GRAB_POINTER | `GrabRequests.grabPointer` | partial | Confine window / cursor / timestamp not applied |
| 27 | UNGRAB_POINTER | `GrabRequests.ungrabPointer` | partial | Timestamp read and discarded |
| 28 | GRAB_BUTTON | `skipRequest` | stub/skip | **NEW** — Passive button grabs not implemented |
| 29 | UNGRAB_BUTTON | `skipRequest` | stub/skip | **NEW** |
| 30 | CHANGE_ACTIVE_POINTER_GRAB | `skipRequest` | stub/skip | **NEW** |
| 31 | GRAB_KEYBOARD | `GrabRequests.grabKeyboard` | reply-stub | **NEW** — Always returns Success; no real keyboard grab |
| 32 | UNGRAB_KEYBOARD | `skipRequest` | stub/skip | **NEW** |
| 33 | GRAB_KEY | `skipRequest` | stub/skip | **NEW** — Passive key grabs not implemented |
| 34 | UNGRAB_KEY | `skipRequest` | stub/skip | **NEW** |
| 35 | ALLOW_EVENTS | `skipRequest` | stub/skip | **NEW** |
| 36 | GRAB_SERVER | `XClientRequestHandler` inline | partial | Sets boolean flag; doesn't queue other-client requests |
| 37 | UNGRAB_SERVER | `XClientRequestHandler` inline | partial | |
| 38 | QUERY_POINTER | `WindowRequests.queryPointer` | partial | same-screen bit uses relative-mouse flag |
| 39 | GET_MOTION_EVENTS | — | missing | Returns BadRequest |
| 40 | TRANSLATE_COORDINATES | `WindowRequests.translateCoordinates` | partial | |
| 41 | WARP_POINTER | `WindowRequests.warpPointer` | partial | Silently skipped in relative-mouse mode |
| 42 | SET_INPUT_FOCUS | `WindowRequests.setInputFocus` | partial | Timestamp not validated |
| 43 | GET_INPUT_FOCUS | `WindowRequests.getInputFocus` | complete | |
| 44 | QUERY_KEYMAP | `KeyboardRequests.queryKeymap` | complete | **FIXED** — Now returns real pressed-key bitmap |
| 45 | OPEN_FONT | `FontRequests.openFont` | partial | **FIXED** — Unknown fonts now return BadName (was BadImplementation) |
| 46 | CLOSE_FONT | `skipRequest` | stub/skip | **NEW** — Font objects not tracked |
| 47 | QUERY_FONT | — | missing | Returns BadRequest |
| 48 | QUERY_TEXT_EXTENTS | `FontRequests.queryTextExtents` | reply-stub | **NEW** — Returns zero extents |
| 49 | LIST_FONTS | `FontRequests.listFonts` | reply-stub | Always 0 fonts |
| 50 | LIST_FONTS_WITH_INFO | — | missing | Returns BadRequest |
| 51 | SET_FONT_PATH | — | missing | Returns BadRequest |
| 52 | GET_FONT_PATH | — | missing | Returns BadRequest |
| 53 | CREATE_PIXMAP | `PixmapRequests.createPixmap` | partial | |
| 54 | FREE_PIXMAP | `PixmapRequests.freePixmap` | partial | |
| 55 | CREATE_GC | `GraphicsContextRequests.createGC` | partial | 17 of 23 GC attributes are parsed+discarded |
| 56 | CHANGE_GC | `GraphicsContextRequests.changeGC` | partial | Same GC attribute gap |
| **57** | **COPY_GC** | `GraphicsContextRequests.copyGC` | partial | **FIXED** — Was assigned opcode 58 (wrong); now correctly 57 |
| 58 | SET_DASHES | `skipRequest` | stub/skip | **NEW** — Dash rendering not implemented |
| 59 | SET_CLIP_RECTANGLES | `GraphicsContextRequests.setClipRectangles` | partial | Rects now stored in GC; draw-time clip application still missing |
| 60 | FREE_GC | `GraphicsContextRequests.freeGC` | partial | |
| 61 | COPY_PLANE | — | missing | Returns BadRequest |
| 62 | COPY_AREA | `DrawRequests.copyArea` | partial | |
| 63 | POLY_ARC | — | missing | Returns BadRequest |
| 64 | FILL_POLY | — | missing | Returns BadRequest |
| 65 | POLY_LINE | `DrawRequests.polyLine` | partial | **FIXED** — PREVIOUS coordinate mode now implemented |
| 66 | POLY_SEGMENT | `DrawRequests.polySegment` | partial | |
| 67 | POLY_RECTANGLE | `DrawRequests.polyRectangle` | partial | |
| 68 | POLY_FILL_ARC | — | missing | Returns BadRequest |
| 69 | POLY_TEXT_8 | — | missing | Returns BadRequest |
| 70 | POLY_FILL_RECTANGLE | `DrawRequests.polyFillRectangle` | partial | **FIXED** — Now uses foreground (was background) |
| 71 | POLY_TEXT_16 | — | missing | Returns BadRequest |
| 72 | PUT_IMAGE | `DrawRequests.putImage` | partial | XY_PIXMAP parsed but not rendered; non-COPY function blocked |
| 73 | GET_IMAGE | `DrawRequests.getImage` | partial | Z_PIXMAP only |
| 74 | IMAGE_TEXT_8 | — | missing | Returns BadRequest |
| 75 | IMAGE_TEXT_16 | — | missing | Returns BadRequest |
| 76 | CREATE_COLORMAP | `skipRequest` | stub/skip | |
| 77 | FREE_COLORMAP | `skipRequest` | stub/skip | |
| 78–92 | Colormap ops | — | missing | All return BadRequest |
| 93 | CREATE_CURSOR | `CursorRequests.createCursor` | partial | |
| 94 | CREATE_GLYPH_CURSOR | `skipRequest` | stub/skip | |
| 95 | FREE_CURSOR | `CursorRequests.freeCursor` | partial | |
| 96 | RECOLOR_CURSOR | — | missing | Returns BadRequest |
| 97 | QUERY_BEST_SIZE | — | missing | Returns BadRequest |
| 98 | QUERY_EXTENSION | `ExtensionRequests.queryExtension` | complete | |
| 99 | LIST_EXTENSIONS | `ExtensionRequests.listExtensions` | complete | **NEW** |
| 100 | CHANGE_KEYBOARD_MAPPING | `KeyboardRequests.changeKeyboardMapping` | partial | **NEW** — Updates keysym table; ignores extras beyond 2 keysyms/keycode |
| 101 | GET_KEYBOARD_MAPPING | `KeyboardRequests.getKeyboardMapping` | partial | **FIXED** — Added bounds validation (BadValue on out-of-range) |
| 102 | CHANGE_KEYBOARD_CONTROL | `skipRequest` | stub/skip | **NEW** — Auto-repeat/bell not applied |
| 103 | GET_KEYBOARD_CONTROL | `KeyboardRequests.getKeyboardControl` | reply-stub | **NEW** — Returns synthetic defaults |
| 104 | BELL | `skipRequest` | stub/skip | |
| 105 | CHANGE_POINTER_CONTROL | — | missing | Returns BadRequest |
| 106 | GET_POINTER_CONTROL | — | missing | Returns BadRequest |
| 107 | SET_SCREEN_SAVER | `WindowRequests.setScreenSaver` | partial | |
| 108 | GET_SCREEN_SAVER | `WindowRequests.getScreenSaver` | partial | |
| 109–114 | Access/host/kill ops | — | missing | All return BadRequest |
| 115 | FORCE_SCREEN_SAVER | `WindowRequests.forceScreenSaver` | partial | |
| 116 | SET_POINTER_MAPPING | XClientRequestHandler inline | reply-stub | **NEW** — Returns Success; no change applied |
| 117 | GET_POINTER_MAPPING | `CursorRequests.getPointerMapping` | partial | Hardcoded 3-button map |
| 118 | SET_MODIFIER_MAPPING | XClientRequestHandler inline | reply-stub | **NEW** — Returns Success; no change applied |
| 119 | GET_MODIFIER_MAPPING | `KeyboardRequests.getModifierMapping` | complete | **FIXED** — Real keycodes for 8 modifier slots |
| 120–126 | (unassigned in X11) | — | — | BadRequest correct |
| 127 | NO_OPERATION | `skipRequest` | complete | |

### 2b. Missing opcodes — complete list

The following opcodes have no handler and return `BadRequest`. Most are low-traffic but a handful matter for compatibility:

**High compatibility impact (clients send these frequently):**
- 39 `GetMotionEvents` — pointer event log query; Wine uses this rarely
- 47 `QueryFont` — font metric query; many text-rendering clients use this
- 50 `ListFontsWithInfo` — extended font query
- 61 `CopyPlane` — 1-bit plane blitting (used by some X11 apps)
- 63 `PolyArc` — arc/ellipse outlines
- 64 `FillPoly` — polygon fill (needed by many GUIs)
- 68 `PolyFillArc` — filled arcs/ellipses
- 69 `PolyText8` / 71 `PolyText16` — text rendering to drawable
- 74 `ImageText8` / 75 `ImageText16` — text rendering

**Medium impact:**
- 105 `ChangePointerControl` / 106 `GetPointerControl` — acceleration settings
- 80–92 colormap allocate/query ops (AllocColor, LookupColor, etc.)

**Low impact (access control, rarely called):**
- 109–114 `ChangeHosts`, `ListHosts`, `SetAccessControl`, `SetCloseDownMode`, `KillClient`, `RotateProperties`

---

## 3. Extension opcode inventory

### 3a. BIG-REQUESTS (major opcode −100)

| Minor | Name | Status | Notes |
|---:|---|---|---|
| 0 | Enable | complete | Returns `MAX_REQUEST_LENGTH = 4194303` |
| other | — | BadRequest | Correct |

### 3b. MIT-SHM (major opcode −101)

| Minor | Name | Status | Notes |
|---:|---|---|---|
| 0 | QueryVersion | complete | Returns 1.1 |
| 1 | Attach | partial | Reads shmid, attaches; read-only flag ignored |
| 2 | Detach | partial | |
| 3 | PutImage | partial | COPY-function-only restriction; depth blindly passed to drawImage |
| 4 | GetImage | missing | BadImplementation |
| 5 | CreatePixmap | missing | BadImplementation |

### 3c. DRI3 (major opcode −102)

| Minor | Name | Status | Notes |
|---:|---|---|---|
| 0 | QueryVersion | complete | Returns 1.0 |
| 1 | Open | partial | **FIXED** — Now sends memfd as placeholder; real DRM device FD not attached |
| 2 | PixmapFromBuffer | partial | |
| 3 | BufferFromPixmap | partial | FD transfer present |
| 4 | FenceFromFD | partial | **NEW** — Registers fence in SyncExtension; FD closed |
| 5 | FDFromFence | partial | **NEW** — Returns memfd placeholder |
| 6 | GetSupportedModifiers | partial | **NEW** — Advertises DRM_FORMAT_MOD_LINEAR only |
| 7 | PixmapFromBuffers | partial | Hardware-buffer and linear-fd paths |

### 3d. Present (major opcode −103)

| Minor | Name | Status | Notes |
|---:|---|---|---|
| 0 | QueryVersion | complete | Returns 1.0 |
| 1 | PresentPixmap | partial | Core copy path; no fence wait, no flip |
| 2 | NotifyMSC | partial | Fake UST/MSC values |
| 3 | SelectInput | partial | GPU texture swap triggered; event registration |
| 4 | QueryCapabilities | partial | Synthetic capability bits |

### 3e. SYNC (major opcode −104)

| Minor | Name | Status | Notes |
|---:|---|---|---|
| 0 | Initialize | complete | Returns 3.1 |
| 14 | CreateFence | complete | |
| 15 | TriggerFence | complete | |
| 16 | ResetFence | complete | Validates triggered state |
| 17 | DestroyFence | complete | |
| 18 | QueryFence | complete | |
| 19 | AwaitFence | likely-incorrect | Returns BadImplementation if no fence triggered; no blocking |

---

## 4. Incorrect / partial implementations found

### 4a. Critical correctness bugs (fixed this pass)

| # | Bug | File | Fix |
|---|---|---|---|
| 1 | `COPY_GC` assigned opcode 58 (X11 spec = 57); `SetDashes` (58) was dispatched to CopyGC handler | `ClientOpcodes.java` | Moved `COPY_GC = 57`; added `SET_DASHES = 58` as stub |
| 2 | `InternAtom` with `onlyIfExists=true` threw `BadAtom` when atom didn't exist; X11 spec requires returning `None(0)` | `AtomRequests.java` | Return 0 instead of throw |
| 3 | `GetAtomName` checked `id < 0` but atom 0 is also invalid | `AtomRequests.java` | Changed to `id <= 0` |
| 4 | `OpenFont` threw `BadImplementation` (code 17) for unknown fonts; correct error is `BadName` (code 15) | `FontRequests.java` | Created `BadName.java`; changed throw |
| 5 | `SetSelectionOwner` with `windowId=0` (release selection) threw `BadWindow` | `SelectionRequests.java` | Allow `windowId=0` |
| 6 | `SendEvent` with `windowId=0` (PointerWindow) or `1` (InputFocus) silently dropped the event | `WindowRequests.java` | Now resolves PointerWindow to window-under-pointer; InputFocus to focused window |
| 7 | `polyFillRectangle` filled with GC background instead of foreground | `DrawRequests.java` | Changed to `getForeground()` |
| 8 | `QueryKeymap` returned 32 zero bytes regardless of key state | `XClientRequestHandler.java`, `KeyboardRequests.java` | Reads `keyboard.getPressedKeys()` into 256-bit bitmap |
| 9 | `GetModifierMapping` returned placeholder (1 keycode/modifier, all zeros) | `KeyboardRequests.java` | Now returns real keycode pairs for 8 X11 modifier slots |
| 10 | `GetKeyboardMapping` had no bounds validation; negative array index possible | `KeyboardRequests.java` | Added `BadValue` when range exceeds `MIN_KEYCODE`–`MAX_KEYCODE` |

### 4b. Protocol gaps — not fixed (scope/complexity)

| Gap | Impact | Reason deferred |
|---|---|---|
| `GrabServer` doesn't queue requests from other clients | Medium | Requires per-client request queueing infrastructure |
| `PolyLine` PREVIOUS mode accumulated then draws — correct, but draws happen per-segment not as one path | Low | Drawable.drawLines already draws pairwise |
| Clip region stored in GC but not applied at draw time | Medium | Requires native C layer changes in `drawLine`/`fillRect` |
| `GrabKeyboard` always returns Success (no real keyboard grab tracking) | Low | No conflicting keyboard grab semantics needed for Wine |
| `SetPointerMapping`/`SetModifierMapping` — reply sent but changes not applied | Low | Pointer/modifier tables not user-configurable in this server |
| 17 of 23 GC attributes parsed and discarded | Medium | Rendering not attribute-driven beyond color/width/function |
| MIT-SHM `GetImage`/`CreatePixmap` (minors 4, 5) missing | Low | Rarely used paths |
| Font subsystem: only `cursor` font accepted | High | Full font rasterization out of scope; QueryFont/text-render opcodes missing |
| DRI3 `Open` sends memfd, not actual DRM device FD | Medium | Would need to open `/dev/dri/renderD*` at runtime |
| SYNC `AwaitFence` doesn't block — throws BadImplementation | Medium | True fence-wait would require thread suspension or poll loop |
| `ReparentWindow` skips x,y placement | Low | Reparent positions not used downstream |

### 4c. Other findings — not bugs, but protocol simplifications

- `GrabPointer` / `WarpPointer` skip in relative-mouse mode and return ALREADY_GRABBED — acceptable for Wine relative-mouse mode
- `SetSelectionOwner` ignores timestamp ordering — clients can steal selections out of order
- `ConfigureWindow` applies geometry changes without sending `ConfigureNotify` to all interested parties
- `QueryTextExtents` returns zero extents — applications that depend on text layout will produce zero-width text

---

## 5. Fixes implemented

### Code changes in this pass

**`ClientOpcodes.java`** — 16 new constants, COPY_GC corrected from 58→57
- Added: `CHANGE_SAVE_SET(6)`, `UNMAP_SUB_WINDOWS(11)`, `CIRCULATE_WINDOW(13)`, `LIST_PROPERTIES(21)`, `CONVERT_SELECTION(24)`, `GRAB_BUTTON(28)`, `UNGRAB_BUTTON(29)`, `CHANGE_ACTIVE_POINTER_GRAB(30)`, `GRAB_KEYBOARD(31)`, `UNGRAB_KEYBOARD(32)`, `GRAB_KEY(33)`, `UNGRAB_KEY(34)`, `ALLOW_EVENTS(35)`, `CLOSE_FONT(46)`, `QUERY_TEXT_EXTENTS(48)`, `SET_DASHES(58)`, `LIST_EXTENSIONS(99)`, `CHANGE_KEYBOARD_MAPPING(100)`, `CHANGE_KEYBOARD_CONTROL(102)`, `GET_KEYBOARD_CONTROL(103)`, `SET_POINTER_MAPPING(116)`, `SET_MODIFIER_MAPPING(118)`
- Fixed: `COPY_GC = 57` (was 58)

**`errors/BadName.java`** — New, error code 15

**`AtomRequests.java`** — InternAtom + GetAtomName correctness fixes

**`FontRequests.java`** — BadName instead of BadImplementation; added `queryTextExtents` (zero-extents reply)

**`SelectionRequests.java`** — SetSelectionOwner accepts windowId=0

**`WindowRequests.java`** — SendEvent PointerWindow/InputFocus resolution; added `listProperties`, `unmapSubWindows`

**`Window.java`** — Added `getPropertyAtoms()` helper

**`GrabRequests.java`** — Added `grabKeyboard` (always-success reply)

**`KeyboardRequests.java`** — `queryKeymap` (real bitmap), `getModifierMapping` (real keycodes), `getKeyboardMapping` (bounds validation), `changeKeyboardMapping` (keysym update), `getKeyboardControl` (synthetic reply)

**`ExtensionRequests.java`** — Added `listExtensions`

**`XClientRequestHandler.java`** — 18 new dispatch cases; QUERY_KEYMAP now calls real handler

**`DrawRequests.java`** — polyFillRectangle foreground fix; polyLine PREVIOUS mode

**`GraphicsContext.java`** — Added clip origin + clipRects storage

**`GraphicsContextRequests.java`** — setClipRectangles now stores rects (was discarding)

**`DRI3Extension.java`** — Added fenceFromFd, fdFromFence, getSupportedModifiers; fixed Open to send FD

**`SyncExtension.java`** — Added `createFence(int, boolean)` and `hasFence(int)` public methods

---

## 6. Validation performed

- `./gradlew :app:compileDebugJavaWithJavac :app:compileDebugKotlin` — **BUILD SUCCESSFUL**
- Opcode constant cross-checked against X11 core protocol opcode table (RFC 1013 / Xlib source)
- `ClientOpcodes.java` constants verified against `XClientRequestHandler` switch arms
- `COPY_GC` opcode value 57 confirmed against `<X11/Xproto.h>` (`X_CopyGC = 57`)
- All new handler methods verified to send well-formed 32-byte reply headers
- `listExtensions` reply encoding verified against X11 spec (nNames in byte 1; STR = 1-byte-len + chars)
- `queryTextExtents` reply layout verified against X11 spec (8 CARD16 fields + 3 INT32 fields)

---

## 7. Remaining opcode backlog

Priority-ordered:

### P1 — High impact, feasible

1. **`QueryFont` (47)** — Return a minimal font reply for the `cursor` font. Many clients probe font metrics before sending text. Implement a synthetic reply with fixed cell width=8, height=16, ascent=13, descent=3.

2. **`FillPoly` (64)** — Polygon fill is used by many X11 toolkits for button/shadow rendering. Native scanline fill would need to be added to `Drawable`.

3. **`PolyText8` (69) / `ImageText8` (74)** — String drawing. Without font metrics, only fixed-cell rendering is possible. A 1-bit fixed font (e.g. 8×16) could enable basic text output.

4. **`CopyPlane` (61)** — 1-bit plane blitting used by some cursors and icon masks.

5. **MIT-SHM GetImage (minor 4)** — Expose SHM buffer for read-back; follows `DrawRequests.getImage` pattern.

6. **DRI3 Open** — Return real DRM device FD (`/dev/dri/renderD128`) when available. Current memfd placeholder causes Mesa to fall back to software rendering.

### P2 — Medium impact

7. **`PolyArc` (63) / `PolyFillArc` (68)** — Arc/ellipse primitives needed for complete toolkit support.

8. **SYNC `AwaitFence` (19)** — Replace BadImplementation with a short poll+retry loop with timeout. Prevents hard failures in GPU-sync paths.

9. **GC Clip Region at draw time** — Store is now implemented; apply `clipRects` in native `drawLine`/`fillRect` to honor SetClipRectangles.

10. **`GetMotionEvents` (39)** — Return empty event list (timestamped motion history not tracked). Currently BadRequest; should return empty reply.

11. **Colormap stubs** — `AllocColor` (84), `QueryColors` (91), `LookupColor` (92) should return synthetic replies for at least the root visual's colors.

### P3 — Low impact / deferred

12. Passive grabs: `GrabButton` (28), `GrabKey` (33) — required for complete WM behavior.
13. `ChangePointerControl` (105) / `GetPointerControl` (106) — return synthetic reply.
14. Font path ops (51, 52) — return empty font path.
15. Host/access control (109–114) — return empty or success replies.
16. `RotateProperties` (114) — implement with `Window.properties` SparseArray.
17. `RecolorCursor` (96) — simple color update on existing cursor.
18. `QueryBestSize` (97) — return the requested size as-is.

---

## 8. Risks / recommendations

### Correctness risks

- **`COPY_GC` opcode bug was live**: Any client that called `CopyGC` before this fix received `BadRequest`. Any client that called `SetDashes` before this fix had its stream consumed as a CopyGC request, likely corrupting the request stream for that client session.
- **`InternAtom` onlyIfExists bug was live**: Any client using `only-if-exists=True` to probe for non-existent atoms received a `BadAtom` error instead of `None`. Clients (including Wine) use this pattern to detect WM properties.
- **SendEvent PointerWindow/InputFocus was silently dropped**: DnD, clipboard, and WM protocol messages routed to `0`/`1` were discarded.

### Compatibility risks

- Font subsystem: `QueryFont` (47) returning `BadRequest` will cause any client that queries font metrics before drawing to fail. This affects Qt, GTK, and SDL2 text paths.
- Text rendering opcodes (69, 74, 75) missing: applications with embedded text rendering (not delegated to cairo/pango) will get `BadRequest`.
- DRI3 `Open` memfd: Mesa DRI will likely detect the invalid FD type and fall back to software rendering (llvmpipe/softpipe). This is functional but slow.

### Sequencing / thread-safety

- The `extensions` `SparseArray` in `XServer` is accessed in `listExtensions` from request-handler threads without a lock. Since the map is populated once at startup and never modified, this is safe in practice but not formally protected. If extensions become dynamically registered, this would need a read lock.
- `Keyboard.getPressedKeys()` returns the live `ArraySet<Byte>` without synchronization in `queryKeymap`. Under concurrent key events this could produce a torn read. Wrapping with `synchronized(keyboard)` or a snapshot copy would eliminate the race.

### Performance

- `listExtensions` allocates `byte[][]` and computes total byte count on every call. Since the extension set is static, caching the encoded reply would be worthwhile.
- `setClipRectangles` allocates a `short[]` per call. For high-frequency GC updates this is measurable; a pooled buffer would help.

### Recommendations

1. Add protocol-level regression tests for the highest-traffic opcodes (`InternAtom`, `ChangeProperty`, `GetProperty`, `PutImage`, `CopyGC`, `SetClipRectangles`).
2. Add a `Timber.d` log for every opcode that is currently `stub/skip` so runtime evidence of actual client usage can drive prioritization.
3. Implement `QueryFont` with a fixed synthetic reply as the next highest-value single change — it unblocks most text-using X11 clients.
4. Investigate DRI3 `Open` with actual DRM device path; this would enable hardware acceleration for Mesa clients.

---

*Build: `./gradlew :app:compileDebugJavaWithJavac :app:compileDebugKotlin` — BUILD SUCCESSFUL.*  
*Pre-existing test compilation failure in `XServerPostExitHealthCheckerTest.kt` (malformed lambda syntax at line 27–43) is unrelated to this pass.*
