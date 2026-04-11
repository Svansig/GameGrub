package com.winlator.xserver;

import android.util.SparseArray;
import com.winlator.core.CursorLocker;
import com.winlator.renderer.GLRenderer;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.extensions.BigReqExtension;
import com.winlator.xserver.extensions.DRI3Extension;
import com.winlator.xserver.extensions.Extension;
import com.winlator.xserver.extensions.MITSHMExtension;
import com.winlator.xserver.extensions.PresentExtension;
import com.winlator.xserver.extensions.SyncExtension;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

/**
 * XServer - The core X11 Window System implementation for this Android application.
 *
 * This is an implementation of the X11 Window System (XServer) that runs on Android devices,
 * allowing Windows applications (particularly games) to run in a virtualized environment.
 *
 * It manages:
 * - Windows, Pixmaps, Graphics Contexts (rendering surfaces)
 * - Input devices (keyboard, mouse/pointer)
 * - Cursor management
 * - X11 extensions for advanced features (DRI3, Present, MIT-SHM, Sync, BigReq)
 * - Screen saver functionality
 * - Input injection from Android to X11 windows
 *
 * The XServer communicates with clients via the XConnector using a custom protocol
 * that implements the X11 request/response format.
 *
 * Key components:
 * - WindowManager: Manages all X11 windows (hierarchical tree structure)
 * - DrawableManager: Manages drawable surfaces (windows, pixmaps)
 * - PixmapManager: Manages offscreen rendering surfaces
 * - GraphicsContextManager: Manages GC objects for rendering parameters
 * - CursorManager: Manages cursor appearances
 * - InputDeviceManager: Manages input device state
 * - SHMSegmentManager: Manages shared memory segments (MIT-SHM extension)
 *
 * @see <a href="https://www.x.org/wiki/X11/">X11 Window System Specification</a>
 */
public class XServer {
    /**
     * Lockable - Enumeration of components that can be locked for thread-safe access.
     *
     * The XServer uses a granular locking strategy where different components can be locked
     * independently to allow concurrent operations on unrelated resources.
     * This prevents deadlock while maintaining thread safety.
     *
     * Common lock combinations:
     * - WINDOW_MANAGER + INPUT_DEVICE: For input event handling and window focus
     * - WINDOW_MANAGER + INPUT_DEVICE + CURSOR_MANAGER: For pointer grabs
     * - PIXMAP_MANAGER + DRAWABLE_MANAGER: For pixmap operations
     * - All: For server-wide operations like grabbing
     */
    public enum Lockable {WINDOW_MANAGER, PIXMAP_MANAGER, DRAWABLE_MANAGER, GRAPHIC_CONTEXT_MANAGER, INPUT_DEVICE, CURSOR_MANAGER, SHMSEGMENT_MANAGER}

    /** X11 protocol version (major version) - X11R11 */
    public static final short VERSION = 11;

    /** Vendor name reported to X11 clients */
    public static final String VENDOR_NAME = "Elbrus Technologies, LLC";

    /** Character set for Latin-1 (ISO-8859-1) text encoding used in X11 */
    public static final Charset LATIN1_CHARSET = Charset.forName("latin1");

    /**
     * Registered X11 extensions indexed by their major opcode.
     * Extensions add additional functionality beyond the core X11 protocol.
     *
     * Supported extensions:
     * - BigReqExtension: Allows requests larger than 256KB
     * - MITSHMExtension: Shared memory for efficient image transfer
     * - DRI3Extension: Direct Rendering Infrastructure
     * - PresentExtension: Buffer presentation/swap
     * - SyncExtension: Synchronization primitives
     */
    public final SparseArray<Extension> extensions = new SparseArray<>();

    /** Screen information (dimensions, DPI, root window visual) */
    public final ScreenInfo screenInfo;

    /** Manages pixmap (offscreen image) allocation and lifecycle */
    public final PixmapManager pixmapManager;

    /**
     * Resource ID allocation system.
     * X11 uses a hierarchical ID system where resource IDs are assigned to clients
     * in ranges, allowing efficient lookups and reference management.
     */
    public final ResourceIDs resourceIDs = new ResourceIDs(128);

    /** Manages Graphics Contexts (GC) - objects holding drawing parameters */
    public final GraphicsContextManager graphicsContextManager = new GraphicsContextManager();

    /** Manages X11 selection (clipboard-like functionality) */
    public final SelectionManager selectionManager;

    /** Manages drawable surfaces (windows and pixmaps) */
    public final DrawableManager drawableManager;

    /** Manages X11 windows in a hierarchical tree structure */
    public final WindowManager windowManager;

    /** Manages cursor appearances (cursor objects and current cursor) */
    public final CursorManager cursorManager;

    /** Keyboard device state and configuration */
    public final Keyboard keyboard = Keyboard.createKeyboard(this);

    /** Pointer (mouse) device state and position */
    public final Pointer pointer = new Pointer(this);

    /** Manages input devices and their state */
    public final InputDeviceManager inputDeviceManager;

    /** Manages pointer/keyboard grabs (exclusive input capture) */
    public final GrabManager grabManager;

    /** Whether the server is currently grabbed by a client */
    private boolean isGrabbed = false;

    /** The client that currently has the server grabbed (null if none) */
    private XClient grabbingClient = null;

    /** Screen saver timeout in seconds (default: 600 = 10 minutes) */
    private short screenSaverTimeout = 600;

    /** Screen saver interval in seconds (default: 600 = 10 minutes) */
    private short screenSaverInterval = 600;

    /** Whether to blank the screen when screen saver activates (default: yes) */
    private byte screenSaverPreferBlanking = 1;

    /** Whether to allow exposures during screen saver (default: yes) */
    private byte screenSaverAllowExposures = 1;

    /** Whether screen saver is currently forced/active */
    private boolean screenSaverForced = false;

    /**
     * Cursor locker - handles Android system cursor visibility.
     * When engaged, hides the Android pointer so the X11 cursor can be displayed instead.
     */
    public final CursorLocker cursorLocker;

    /** Shared memory segment manager (MIT-SHM extension) */
    private SHMSegmentManager shmSegmentManager;

    /**
     * OpenGL ES renderer for drawing X11 windows.
     * The renderer takes window contents and draws them to the Android display.
     */
    private GLRenderer renderer;

    /**
     * WinHandler - interface to Windows applications running in containers.
     * Used to send input events and receive window state changes.
     */
    private WinHandler winHandler;

    /**
     * Lock map - ReentrantLock for each lockable component.
     * Using individual locks allows concurrent access to unrelated components.
     */
    private final EnumMap<Lockable, ReentrantLock> locks = new EnumMap<>(Lockable.class);

    /**
     * Whether to use relative mouse movement.
     * When true, mouse movement is relative (delta) rather than absolute position.
     * Used for games that need raw mouse input (FPS games, etc.)
     */
    private boolean relativeMouseMovement = false;

    /** Whether to simulate touch screen events as pointer clicks */
    private boolean simulateTouchScreen = false;


    /**
     * Constructs a new XServer instance.
     *
     * @param screenInfo Screen dimensions and visual information for the display
     */
    public XServer(ScreenInfo screenInfo) {
        Timber.tag("XServer").d("Creating xServer %s", screenInfo);
        this.screenInfo = screenInfo;
        cursorLocker = new CursorLocker(this);

        // Initialize a lock for each lockable component
        for (Lockable lockable : Lockable.values()) locks.put(lockable, new ReentrantLock());

        // Initialize managers for different resource types
        pixmapManager = new PixmapManager();
        drawableManager = new DrawableManager(this);
        cursorManager = new CursorManager(drawableManager);
        windowManager = new WindowManager(screenInfo, drawableManager);
        selectionManager = new SelectionManager(windowManager);
        inputDeviceManager = new InputDeviceManager(this);
        grabManager = new GrabManager(this);

        // Set up desktop environment integration
        DesktopHelper.attachTo(this);

        // Initialize X11 protocol extensions
        setupExtensions();
    }

    public boolean isRelativeMouseMovement() {
        return relativeMouseMovement;
    }

    /**
     * Sets relative mouse movement mode.
     * In relative mode, mouse movement is reported as deltas (change in position)
     * instead of absolute coordinates. Used by games that need raw input.
     *
     * @param relativeMouseMovement true for relative, false for absolute
     */
    public void setRelativeMouseMovement(boolean relativeMouseMovement) {
        // Disable cursor locker when using relative mode (game handles its own cursor)
        cursorLocker.setEnabled(!relativeMouseMovement);
        this.relativeMouseMovement = relativeMouseMovement;
    }

    /**
     * @return true if touch screen simulation is enabled
     */
    public boolean isSimulateTouchScreen() { return simulateTouchScreen; }

    /**
     * Sets whether to simulate touch screen events from pointer events.
     * When enabled, pointer clicks are sent as touch events (for apps that use touch).
     *
     * @param simulateTouchScreen true to enable touch simulation
     */
    public void setSimulateTouchScreen(boolean simulateTouchScreen) {
        this.simulateTouchScreen = simulateTouchScreen;
    }

    /**
     * @return The OpenGL ES renderer for this XServer
     */
    public GLRenderer getRenderer() {
        return renderer;
    }

    /**
     * Sets the OpenGL ES renderer used to draw window contents.
     *
     * @param renderer The GLRenderer instance
     */
    public void setRenderer(GLRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * @return The WinHandler for Windows application communication
     */
    public WinHandler getWinHandler() {
        return winHandler;
    }

    /**
     * Sets the WinHandler for Windows application communication.
     * Used to send input to Windows apps and receive window updates.
     *
     * @param winHandler The WinHandler instance
     */
    public void setWinHandler(WinHandler winHandler) {
        this.winHandler = winHandler;
    }

    /**
     * @return The shared memory segment manager (may be null if MIT-SHM not used)
     */
    public SHMSegmentManager getSHMSegmentManager() {
        return shmSegmentManager;
    }

    /**
     * Sets the shared memory segment manager for MIT-SHM extension.
     *
     * @param shmSegmentManager The SHMSegmentManager instance
     */
    public void setSHMSegmentManager(SHMSegmentManager shmSegmentManager) {
        this.shmSegmentManager = shmSegmentManager;
    }

    /**
     * XLock implementation for locking a single resource type.
     * Uses try-with-resources pattern for automatic unlocking.
     */
    private class SingleXLock implements XLock {
        private final ReentrantLock lock;

        private SingleXLock(Lockable lockable) {
            this.lock = locks.get(lockable);
            Objects.requireNonNull(lock).lock();
        }

        @Override
        public void close() {
            lock.unlock();
        }
    }

    /**
     * XLock implementation for locking multiple resource types.
     * Acquires all locks in order to prevent deadlock, releases in reverse order.
     * Uses try-with-resources pattern for automatic unlocking.
     */
    private class MultiXLock implements XLock {
        private final Lockable[] lockables;

        private MultiXLock(Lockable[] lockables) {
            this.lockables = lockables;
            // Acquire locks in forward order
            for (Lockable lockable : lockables) Objects.requireNonNull(locks.get(lockable)).lock();
        }

        @Override
        public void close() {
            // Release locks in reverse order for safety
            for (int i = lockables.length - 1; i >= 0; i--) {
                Objects.requireNonNull(locks.get(lockables[i])).unlock();
            }
        }
    }

    /**
     * Acquires a lock on a single resource type.
     *
     * @param lockable The resource type to lock
     * @return XLock that should be used in try-with-resources
     */
    public XLock lock(Lockable lockable) {
        return new SingleXLock(lockable);
    }

    /**
     * Acquires locks on multiple resource types atomically.
     *
     * @param lockables The resource types to lock
     * @return XLock that should be used in try-with-resources
     */
    public XLock lock(Lockable... lockables) {
        return new MultiXLock(lockables);
    }

    /**
     * Acquires all locks - used for server-wide operations.
     *
     * @return XLock that should be used in try-with-resources
     */
    public XLock lockAll() {
        return new MultiXLock(Lockable.values());
    }

    /**
     * Finds an extension by its name.
     *
     * @param name The extension name (e.g., "MIT-SHM", "Present", "DRI3")
     * @return The extension, or null if not found
     */
    public Extension getExtensionByName(String name) {
        for (int i = 0; i < extensions.size(); i++) {
            Extension extension = extensions.valueAt(i);
            if (extension.getName().equals(name)) return extension;
        }
        return null;
    }

    /**
     * Injects a pointer move to an absolute position.
     * Used to send mouse movement from Android input to X11 clients.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     */
    public void injectPointerMove(int x, int y) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(x, y);
        }
    }

    /**
     * Injects relative pointer movement (delta).
     * Used for games that need relative mouse movement.
     *
     * @param dx Change in X coordinate
     * @param dy Change in Y coordinate
     */
    public void injectPointerMoveDelta(int dx, int dy) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setPosition(pointer.getX() + dx, pointer.getY() + dy);
        }
    }

    /**
     * Injects a pointer button press (mouse button down).
     *
     * @param buttonCode The button being pressed
     */
    public void injectPointerButtonPress(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, true);
        }
    }

    /**
     * Injects a pointer button release (mouse button up).
     *
     * @param buttonCode The button being released
     */
    public void injectPointerButtonRelease(Pointer.Button buttonCode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            pointer.setButton(buttonCode, false);
        }
    }

    /**
     * Injects a key press without keysym translation.
     *
     * @param xKeycode The X11 keycode
     */
    public void injectKeyPress(XKeycode xKeycode) {
        injectKeyPress(xKeycode, 0);
    }

    /**
     * Injects a key press with explicit keysym.
     * Used to send keyboard input from Android to X11 clients.
     *
     * @param xKeycode The X11 keycode
     * @param keysym The translated keysym (0 for none)
     */
    public void injectKeyPress(XKeycode xKeycode, int keysym) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyPress(xKeycode.getId(), keysym);
        }
    }

    /**
     * Injects a key release.
     *
     * @param xKeycode The X11 keycode
     */
    public void injectKeyRelease(XKeycode xKeycode) {
        try (XLock lock = lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE)) {
            keyboard.setKeyRelease(xKeycode.getId());
        }
    }

    /**
     * Sets up X11 protocol extensions.
     * Extensions must be registered with their major opcode for request routing.
     */
    private void setupExtensions() {
        extensions.put(BigReqExtension.MAJOR_OPCODE, new BigReqExtension());
        extensions.put(MITSHMExtension.MAJOR_OPCODE, new MITSHMExtension());
        extensions.put(DRI3Extension.MAJOR_OPCODE, new DRI3Extension());
        extensions.put(PresentExtension.MAJOR_OPCODE, new PresentExtension());
        extensions.put(SyncExtension.MAJOR_OPCODE, new SyncExtension());
    }

    /**
     * Gets an extension by its major opcode.
     *
     * @param opcode The extension's major opcode
     * @return The extension cast to its type, or null if not found
     */
    public <T extends Extension> T getExtension(int opcode) {
        return (T)extensions.get(opcode);
    }

    /**
     * Sets whether the server is grabbed by a client.
     * When grabbed, only that client receives input and can make requests.
     *
     * @param grabbed true to grab, false to release
     * @param client The client grabbing (or null to release)
     */
    public synchronized void setGrabbed(boolean grabbed, XClient client) {
        this.isGrabbed = grabbed;
        this.grabbingClient = client;
    }

    /**
     * Checks if the server is grabbed by a specific client.
     *
     * @param client The client to check
     * @return true if grabbed by this client
     */
    public synchronized boolean isGrabbedBy(XClient client) {
        if (this.isGrabbed) {
            return this.grabbingClient == client;
        }
        return false;
    }

    /**
     * Configures screen saver behavior.
     *
     * @param timeout Seconds before screen saver activates
     * @param interval Interval for screen saver animation
     * @param preferBlanking Whether to blank screen (1=yes, 0=no)
     * @param allowExposures Whether to allow exposure events (1=yes, 0=no)
     */
    public synchronized void setScreenSaverSettings(short timeout, short interval, byte preferBlanking, byte allowExposures) {
        this.screenSaverTimeout = timeout;
        this.screenSaverInterval = interval;
        this.screenSaverPreferBlanking = preferBlanking;
        this.screenSaverAllowExposures = allowExposures;
    }

    /**
     * @return Seconds before screen saver activates
     */
    public synchronized short getScreenSaverTimeout() {
        return screenSaverTimeout;
    }

    /**
     * @return Screen saver animation interval in seconds
     */
    public synchronized short getScreenSaverInterval() {
        return screenSaverInterval;
    }

    /**
     * @return Whether to blank screen when saver active (1=yes, 0=no)
     */
    public synchronized byte getScreenSaverPreferBlanking() {
        return screenSaverPreferBlanking;
    }

    /**
     * @return Whether to allow exposure events during saver (1=yes, 0=no)
     */
    public synchronized byte getScreenSaverAllowExposures() {
        return screenSaverAllowExposures;
    }

    /**
     * Forces the screen saver to be active or inactive.
     *
     * @param forced true to force active, false for normal
     */
    public synchronized void setScreenSaverForced(boolean forced) {
        this.screenSaverForced = forced;
    }

    /**
     * @return true if screen saver is currently forced
     */
    public synchronized boolean isScreenSaverForced() {
        return screenSaverForced;
    }

}
