# X11/Xorg Opcode Audit (2026-04-09)

## 1. Dispatch entrypoints

- Core request dispatch: `app/src/main/java/com/winlator/xserver/XClientRequestHandler.java` (`handleNormalRequest` switch on major opcode).
- Core opcode declarations: `app/src/main/java/com/winlator/xserver/ClientOpcodes.java`.
- Extension registration: `app/src/main/java/com/winlator/xserver/XServer.java` (`setupExtensions`).
- Extension discovery reply path: `app/src/main/java/com/winlator/xserver/requests/ExtensionRequests.java` (`queryExtension`).
- Extension dispatch: each `Extension.handleRequest(...)` in `app/src/main/java/com/winlator/xserver/extensions/*Extension.java`.
- Protocol error emission: `app/src/main/java/com/winlator/xserver/errors/XRequestError.java` (`sendError`).

## 2. Core opcode inventory

Legend: `complete`, `partial`, `stub/skip`, `missing`.

### Implemented/advertised opcodes (`ClientOpcodes`)

| Opcode | Name | Handler | Status | Notes |
|---:|---|---|---|---|
| 1 | CREATE_WINDOW | `WindowRequests.createWindow` | partial | Core behavior present; no explicit request-length sanity checks per field count. |
| 2 | CHANGE_WINDOW_ATTRIBUTES | `WindowRequests.changeWindowAttributes` | partial | Event mask access checks present; many attribute bits are ignored in lower layers. |
| 3 | GET_WINDOW_ATTRIBUTES | `WindowRequests.getWindowAttributes` | partial | Reply generated; some fields are hardcoded/default-oriented. |
| 4 | DESTROY_WINDOW | `WindowRequests.destroyWindow` | partial | State/event mutation present. |
| 5 | DESTROY_SUB_WINDOW | `WindowRequests.destroySubWindows` | partial | Child iteration implemented. |
| 7 | REPARENT_WINDOW | `WindowRequests.reparentWindow` | partial | Reparent semantics simplified (no events/reconfiguration parity). |
| 8 | MAP_WINDOW | `WindowRequests.mapWindow` | partial | Map notify/expose paths present. |
| 9 | MAP_SUB_WINDOW | `WindowRequests.mapSubWindows` | partial | Recursive map path present. |
| 10 | UNMAP_WINDOW | `WindowRequests.unmapWindow` | partial | Unmap notify path present. |
| 12 | CONFIGURE_WINDOW | `WindowRequests.configureWindow` | partial | Configure flow exists; validation breadth limited. |
| 14 | GET_GEOMETRY | `WindowRequests.getGeometry` | partial | Reply emitted; root/depth assumptions simplified. |
| 15 | QUERY_TREE | `WindowRequests.queryTree` | partial | Reply emitted. |
| 16 | INTERN_ATOM | `AtomRequests.internAtom` | partial | Basic behavior present. |
| 17 | GET_ATOM_NAME | `AtomRequests.getAtomName` | partial | Atom validation incomplete (`id < 0` check only). |
| 18 | CHANGE_PROPERTY | `WindowRequests.changeProperty` | partial | Mutation path present; limited type/format validation. |
| 19 | DELETE_PROPERTY | `WindowRequests.deleteProperty` | partial | Basic behavior present. |
| 20 | GET_PROPERTY | `WindowRequests.getProperty` | partial | Reply logic present; edge-case type/offset behavior simplified. |
| 22 | SET_SELECTION_OWNER | `SelectionRequests.setSelectionOwner` | partial | Timestamp policy simplified. |
| 23 | GET_SELECTION_OWNER | `SelectionRequests.getSelectionOwner` | partial | Reply path present. |
| 25 | SEND_EVENT | `WindowRequests.sendEvent` | partial | Paths for destination/event mask exist; special-window handling is non-standard. |
| 26 | GRAB_POINTER | `GrabRequests.grabPointer` | partial | Status replies exist; timestamp/confine/cursor handling reduced. |
| 27 | UNGRAB_POINTER | `GrabRequests.ungrabPointer` | partial | Basic ungrab path. |
| 36 | GRAB_SERVER | `XClientRequestHandler` inline | partial | Fixed incorrect success reply for void request in this pass. |
| 37 | UNGRAB_SERVER | `XClientRequestHandler` inline | partial | Fixed incorrect success reply for void request in this pass. |
| 38 | QUERY_POINTER | `WindowRequests.queryPointer` | partial | Reply path present. |
| 40 | TRANSLATE_COORDINATES | `WindowRequests.translateCoordinates` | partial | Reply path present. |
| 41 | WARP_POINTER | `WindowRequests.warpPointer` | partial | Relative-mode and geometry checks simplified. |
| 42 | SET_INPUT_FOCUS | `WindowRequests.setInputFocus` | partial | Revert-to handled; timestamp policy omitted. |
| 43 | GET_INPUT_FOCUS | `WindowRequests.getInputFocus` | partial | Reply path present. |
| 44 | QUERY_KEYMAP | `XClientRequestHandler` inline | likely incorrect | Returns zeroed keymap (placeholder-like). |
| 45 | OPEN_FONT | `FontRequests.openFont` | partial | Only `cursor` font accepted; now returns protocol error instead of runtime exception. |
| 49 | LIST_FONTS | `FontRequests.listFonts` | stub/skip | Always returns 0 fonts. |
| 53 | CREATE_PIXMAP | `PixmapRequests.createPixmap` | partial | Basic create path present. |
| 54 | FREE_PIXMAP | `PixmapRequests.freePixmap` | partial | Basic free path present. |
| 55 | CREATE_GC | `GraphicsContextRequests.createGC` | partial | Basic create path present; many GC attributes ignored globally. |
| 56 | CHANGE_GC | `GraphicsContextRequests.changeGC` | partial | Many bits are parsed then skipped. |
| 58 | COPY_GC | `GraphicsContextRequests.copyGC` | partial | Many bits unsupported. |
| 59 | SET_CLIP_RECTANGLES | `GraphicsContextRequests.setClipRectangles` | partial | Implemented this pass as parse/validate only; clipping still not applied during draw. |
| 60 | FREE_GC | `GraphicsContextRequests.freeGC` | partial | Basic free path present. |
| 62 | COPY_AREA | `DrawRequests.copyArea` | partial | Basic copy path present. |
| 65 | POLY_LINE | `DrawRequests.polyLine` | partial | `CoordinateMode.PREVIOUS` not implemented. |
| 66 | POLY_SEGMENT | `DrawRequests.polySegment` | partial | Implemented this pass using line raster path. |
| 67 | POLY_RECTANGLE | `DrawRequests.polyRectangle` | partial | Implemented this pass using 4-line rectangle outlines. |
| 70 | POLY_FILL_RECTANGLE | `DrawRequests.polyFillRectangle` | partial | Uses GC background color (likely wrong for many clients expecting foreground). |
| 72 | PUT_IMAGE | `DrawRequests.putImage` | partial | Only limited formats/functions supported. |
| 73 | GET_IMAGE | `DrawRequests.getImage` | partial | `Z_PIXMAP` only; other formats error. |
| 78 | CREATE_COLORMAP | `XClientRequestHandler` (`skipRequest`) | stub/skip | Silently ignored. |
| 79 | FREE_COLORMAP | `XClientRequestHandler` (`skipRequest`) | stub/skip | Silently ignored. |
| 93 | CREATE_CURSOR | `CursorRequests.createCursor` | partial | Basic pixmap cursor path present. |
| 94 | CREATE_GLYPH_CURSOR | `XClientRequestHandler` (`skipRequest`) | stub/skip | Silently ignored. |
| 95 | FREE_CURSOR | `CursorRequests.freeCursor` | partial | Basic free path present. |
| 98 | QUERY_EXTENSION | `ExtensionRequests.queryExtension` | partial | Basic extension advert/query present. |
| 101 | GET_KEYBOARD_MAPPING | `KeyboardRequests.getKeyboardMapping` | partial | No robust bounds checks for keycode/count. |
| 104 | BELL | `XClientRequestHandler` (`skipRequest`) | stub/skip | Silently ignored. |
| 107 | SET_SCREEN_SAVER | `WindowRequests.setScreenSaver` | partial | Implemented this pass with state persistence + validation. |
| 108 | GET_SCREEN_SAVER | `WindowRequests.getScreenSaver` | partial | Now returns persisted values. |
| 115 | FORCE_SCREEN_SAVER | `WindowRequests.forceScreenSaver` | partial | Implemented this pass as validated state toggle. |
| 117 | GET_POINTER_MAPPING | `CursorRequests.getPointerMapping` | partial | Hardcoded 3-button mapping. |
| 119 | GET_MODIFIER_MAPPING | `KeyboardRequests.getModifierMapping` | partial | Fixed-size placeholder mapping. |
| 127 | NO_OPERATION | `XClientRequestHandler` (`skipRequest`) | complete | Correct no-op behavior. |

### Core opcodes currently missing from this server

- All major opcodes not listed in `ClientOpcodes` are currently `missing` and return `BadRequest` after this pass (previously unknown major opcodes were logged and ignored).

## 3. Extension opcode inventory

### Registered extensions (`XServer.setupExtensions`)

| Extension | Major opcode | Dispatch file | Overall status |
|---|---:|---|---|
| BIG-REQUESTS | -100 | `extensions/BigReqExtension.java` | partial |
| MIT-SHM | -101 | `extensions/MITSHMExtension.java` | partial |
| DRI3 | -102 | `extensions/DRI3Extension.java` | partial |
| Present | -103 | `extensions/PresentExtension.java` | partial |
| SYNC | -104 | `extensions/SyncExtension.java` | partial |

### Extension minor opcode coverage

| Extension | Minor | Name | Status | Notes |
|---|---:|---|---|---|
| BIG-REQUESTS | 0 | Enable | partial | Reply shape exists; unsupported minors now `BadRequest`. |
| MIT-SHM | 0 | QUERY_VERSION | partial | Reply present. |
| MIT-SHM | 1 | ATTACH | partial | Attach path present. |
| MIT-SHM | 2 | DETACH | partial | Detach path present. |
| MIT-SHM | 3 | PUT_IMAGE | partial | Draw path exists; unsupported GC function now protocol error. |
| DRI3 | 0 | QUERY_VERSION | partial | Reply present. |
| DRI3 | 1 | OPEN | likely incorrect | Returns reply without FD payload expected by real clients. |
| DRI3 | 2 | PIXMAP_FROM_BUFFER | partial | Basic import path present. |
| DRI3 | 3 | BUFFER_FROM_PIXMAP | partial | Export path present with FD transfer. |
| DRI3 | 4 | FENCE_FROM_FD | missing | Declared but not dispatched. |
| DRI3 | 5 | FD_FROM_FENCE | missing | Declared but not dispatched. |
| DRI3 | 6 | GET_SUPPORTED_FORMATS | missing | Declared but not dispatched. |
| DRI3 | 7 | PIXMAP_FROM_BUFFERS | partial | Added error on unsupported modifiers (was silent no-op). |
| Present | 0 | QUERY_VERSION | partial | Reply present. |
| Present | 1 | PRESENT_PIXMAP | partial | Core copy path/events present; timing/fence semantics simplified. |
| Present | 2 | NOTIFY_MSC | partial | Fake MSC/UST values. |
| Present | 3 | SELECT_INPUT | partial | Event registration path present. |
| Present | 4 | QUERY_CAPABILITIES | partial | Capability bits are synthetic. |
| SYNC | 0 | INITIALIZE | partial | Reply present. |
| SYNC | 14 | CREATE_FENCE | partial | Fence map created. |
| SYNC | 15 | TRIGGER_FENCE | partial | Trigger path present. |
| SYNC | 16 | RESET_FENCE | partial | Reset path present. |
| SYNC | 17 | DESTROY_FENCE | partial | Destroy path present. |
| SYNC | 18 | QUERY_FENCE | partial | Reply present. |
| SYNC | 19 | AWAIT_FENCE | likely incorrect | Non-triggered fences return `BadImplementation` instead of blocking semantics. |

## 4. Incorrect / partial implementations found

High-impact gaps found during this pass:

1. Unknown major opcodes were silently ignored in `XClientRequestHandler` (now fixed to `BadRequest`).
2. `GrabServer` / `UngrabServer` incorrectly emitted success replies for void requests (now fixed).
3. Unsupported draw paths (`PutImage`/`GetImage`, MIT-SHM `PutImage`, `OpenFont`) threw unchecked runtime exceptions instead of X errors (now fixed to protocol errors).
4. Multiple opcodes were skip-only placeholders (`SET_CLIP_RECTANGLES`, `POLY_SEGMENT`, `POLY_RECTANGLE`, `SET_SCREEN_SAVER`, `FORCE_SCREEN_SAVER`) and now have targeted handlers.
5. DRI3 `PixmapFromBuffers` silently succeeded on unsupported modifiers (now fixed to explicit protocol error).

Important remaining correctness/compatibility issues:

- `QUERY_KEYMAP`, `LIST_FONTS`, `GET_POINTER_MAPPING`, `GET_MODIFIER_MAPPING` are placeholder-like.
- DRI3 advertises features whose minors are not implemented (`4`, `5`, `6`), and `OPEN` reply likely lacks required FD semantics.
- Present/SYNC timing/fence behavior is heavily simplified versus spec.
- Colormap and glyph cursor paths are still skip-only.

## 5. Fixes implemented

### Code changes in this pass

- Added `BadRequest` protocol error class:
  - `app/src/main/java/com/winlator/xserver/errors/BadRequest.java`
- Core dispatch hardening and void-request reply correction:
  - `app/src/main/java/com/winlator/xserver/XClientRequestHandler.java`
- Draw request protocol-error behavior and new drawing handlers:
  - `app/src/main/java/com/winlator/xserver/requests/DrawRequests.java`
- BIG-REQUESTS minor-opcode validation:
  - `app/src/main/java/com/winlator/xserver/extensions/BigReqExtension.java`
- MIT-SHM unsupported function error-path fix:
  - `app/src/main/java/com/winlator/xserver/extensions/MITSHMExtension.java`
- `OpenFont` unsupported-name error-path fix:
  - `app/src/main/java/com/winlator/xserver/requests/FontRequests.java`
- `SetClipRectangles` parse/validate handler:
  - `app/src/main/java/com/winlator/xserver/requests/GraphicsContextRequests.java`
- Screen saver state persistence + force mode support:
  - `app/src/main/java/com/winlator/xserver/XServer.java`
  - `app/src/main/java/com/winlator/xserver/requests/WindowRequests.java`
- DRI3 unsupported modifier path now errors instead of silent success:
  - `app/src/main/java/com/winlator/xserver/extensions/DRI3Extension.java`

## 6. Validation performed

- Built module after changes:
  - `./gradlew :app:compileDebugKotlin`
  - Result: success.
- Static coverage check performed by extracting dispatch case tables from:
  - `ClientOpcodes.java`
  - `XClientRequestHandler.java`
  - Extension classes in `extensions/`.

## 7. Remaining opcode backlog

Priority-ordered follow-up work for compatibility/performance:

1. DRI3 protocol parity
   - Implement minors `FENCE_FROM_FD` (4), `FD_FROM_FENCE` (5), `GET_SUPPORTED_FORMATS` (6).
   - Correct `OPEN` reply to transfer expected FD(s).
2. Core placeholder requests
   - Implement `CREATE_GLYPH_CURSOR`, `CREATE_COLORMAP`, `FREE_COLORMAP`, `BELL` behavior.
3. Graphics correctness
   - Implement actual clip region storage + draw-time clip application for `SET_CLIP_RECTANGLES`.
   - Add `CoordinateMode.PREVIOUS` support in `POLY_LINE`.
4. Keyboard/input correctness
   - Replace placeholder replies for `QUERY_KEYMAP` and `GET_MODIFIER_MAPPING`.
   - Add bounds validation in `GET_KEYBOARD_MAPPING`.
5. Sync/Present semantics
   - Replace `SYNC AWAIT_FENCE` fake behavior with real wait/trigger model.
   - Improve Present timing/capability/event sequencing for real client compatibility.
6. Request framing robustness
   - Add strict per-request length validation helpers to prevent malformed parsing drift.

## 8. Risks / recommendations

- Compatibility risk remains highest in DRI3/Present/SYNC where extension support is advertised but only partially spec-compliant.
- Rendering correctness risk remains in clipping and certain primitive operations (partial geometry semantics).
- Performance recommendation: keep lock scopes narrow (already mostly true), avoid `lockAll()` except where mandatory, and prefer zero-copy buffer paths already used by DRI3/MIT-SHM.
- Recommendation: add protocol-level request/response tests for high-traffic opcodes (`PutImage`, `GetImage`, `SetClipRectangles`, DRI3 open/import/export, Sync fences) to prevent regressions.

Documentation Impact: Added this audit report to document current opcode coverage, fixes, and remaining protocol backlog.

