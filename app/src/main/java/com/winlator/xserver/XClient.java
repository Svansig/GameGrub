package com.winlator.xserver;

import androidx.collection.ArrayMap;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.events.Event;

import java.io.IOException;
import java.util.ArrayList;

import timber.log.Timber;

/**
 * XClient - Represents a connected X11 client (application).
 * 
 * Each XClient represents an application connection to the XServer.
 * The client has:
 * - Unique resource ID base (assigned range for its resources)
 * - Input/output streams for X11 protocol communication
 * - Registered interest in window events (EventListener)
 * - Ownership of resources (Windows, Pixmaps, GCs, Cursors)
 * 
 * X11 uses a request-response model where each request generates a reply.
 * Clients request window creation, drawing operations, input config, etc.
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Protocol Specification</a>
 */
public class XClient implements XResourceManager.OnResourceLifecycleListener {
    /** The XServer this client is connected to */
    public final XServer xServer;
    
    /** Whether the client has completed X11 authentication (completed handshake) */
    private boolean authenticated = false;
    
    /** 
     * Base resource ID for this client's resources.
     * The client can create resources with IDs in a range starting at this base.
     * This allows efficient ownership validation.
     */
    public final Integer resourceIDBase;
    
    /** Sequence number incremented for each request (for reply matching) */
    private short sequenceNumber = 0;
    
    /** Length of current request in bytes */
    private int requestLength;
    
    /** Request data byte (second byte of request, contains request-specific data) */
    private byte requestData;
    
    /** Available input bytes when request started (for length tracking) */
    private int initialLength;
    
    /** Input stream for reading X11 requests */
    private final XInputStream inputStream;
    
    /** Output stream for writing X11 replies and events */
    private final XOutputStream outputStream;
    
    /** Whether the output stream has been closed (client disconnected) */
    private boolean outputClosed = false;
    
    /**
     * Event listeners registered for windows.
     * Maps window to the listener that filters/delegates events for that window.
     */
    private final ArrayMap<Window, EventListener> eventListeners = new ArrayMap<>();
    
    /** Resources owned by this client (for cleanup on disconnect) */
    private final ArrayList<XResource> resources = new ArrayList<>();

    /**
     * Creates a new XClient for a connection.
     * 
     * @param xServer The XServer this client connects to
     * @param inputStream Stream for reading requests
     * @param outputStream Stream for writing replies/events
     */
    public XClient(XServer xServer, XInputStream inputStream, XOutputStream outputStream) {
        this.xServer = xServer;
        this.inputStream = inputStream;
        this.outputStream = outputStream;

        try (XLock lock = xServer.lockAll()) {
            // Allocate a resource ID range for this client
            resourceIDBase = xServer.resourceIDs.get();
            
            // Register for resource lifecycle events (automatic cleanup)
            xServer.windowManager.addOnResourceLifecycleListener(this);
            xServer.pixmapManager.addOnResourceLifecycleListener(this);
            xServer.graphicsContextManager.addOnResourceLifecycleListener(this);
            xServer.cursorManager.addOnResourceLifecycleListener(this);
        }
    }

    /**
     * Registers ownership of a resource.
     * Used to track resources for cleanup when client disconnects.
     * 
     * @param resource The resource to track
     */
    public void registerAsOwnerOfResource(XResource resource) {
        resources.add(resource);
    }

    /**
     * Sets event listener for a window.
     * The client will receive events matching the event mask for that window.
     * 
     * @param window The window to listen to
     * @param eventMask The events the client wants to receive
     */
    public void setEventListenerForWindow(Window window, Bitmask eventMask) {
        // Remove existing listener if any
        EventListener eventListener = eventListeners.get(window);
        if (eventListener != null) window.removeEventListener(eventListener);
        if (eventMask.isEmpty()) return;
        
        // Create new listener
        eventListener = new EventListener(this, eventMask);
        eventListeners.put(window, eventListener);
        window.addEventListener(eventListener);
    }

    /**
     * Sends an event to this client.
     * Used to forward X11 events (key presses, mouse movement, window changes).
     * 
     * @param event The event to send
     */
    public void sendEvent(Event event) {
        if (outputClosed) {
            return;
        }
        try {
            event.send(sequenceNumber, outputStream);
        }
        catch (IOException e) {
            outputClosed = true;
            Timber.tag("XClient").d("Dropping X11 events for disconnected client");
        }
    }

    /**
     * Checks if this client is interested in a specific event for a window.
     * 
     * @param eventId The event type ID
     * @param window The window to check
     * @return true if interested
     */
    public boolean isInterestedIn(int eventId, Window window) {
        EventListener eventListener = eventListeners.get(window);
        return eventListener != null && eventListener.isInterestedIn(eventId);
    }

    /**
     * @return true if authentication handshake is complete
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Sets authentication state.
     * 
     * @param authenticated true after successful handshake
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    /**
     * Frees all resources owned by this client.
     * Called when client disconnects to clean up its resources.
     */
    public void freeResources() {
        try (XLock lock = xServer.lockAll()) {
            // Free all tracked resources
            while (!resources.isEmpty()) {
                XResource resource = resources.remove(resources.size()-1);
                if (resource instanceof Window) {
                    xServer.windowManager.destroyWindow(resource.id);
                }
                else if (resource instanceof Pixmap) {
                    xServer.pixmapManager.freePixmap(resource.id);
                }
                else if (resource instanceof GraphicsContext) {
                    xServer.graphicsContextManager.freeGraphicsContext(resource.id);
                }
                else if (resource instanceof Cursor) {
                    xServer.cursorManager.freeCursor(resource.id);
                }
            }

            // Remove all event listeners
            while (!eventListeners.isEmpty()) {
                int i = eventListeners.size()-1;
                eventListeners.keyAt(i).removeEventListener(eventListeners.removeAt(i));
            }

            // Unregister lifecycle listeners
            xServer.windowManager.removeOnResourceLifecycleListener(this);
            xServer.pixmapManager.removeOnResourceLifecycleListener(this);
            xServer.graphicsContextManager.removeOnResourceLifecycleListener(this);
            xServer.cursorManager.removeOnResourceLifecycleListener(this);
            
            // Free the resource ID range
            xServer.resourceIDs.free(resourceIDBase);
        }
    }

    /**
     * Increments sequence number for request tracking.
     */
    public void generateSequenceNumber() {
        sequenceNumber++;
    }

    /**
     * @return Current sequence number
     */
    public short getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * @return Current request length
     */
    public int getRequestLength() {
        return requestLength;
    }

    /**
     * Sets request length and initial input state.
     * 
     * @param requestLength The request length
     */
    public void setRequestLength(int requestLength) {
        this.requestLength = requestLength;
        initialLength = inputStream.available();
    }

    /**
     * @return Request data byte
     */
    public byte getRequestData() {
        return requestData;
    }

    /**
     * Sets request data byte.
     * 
     * @param requestData The request data
     */
    public void setRequestData(byte requestData) {
        this.requestData = requestData;
    }

    /**
     * @return Remaining bytes in current request
     */
    public int getRemainingRequestLength() {
        int actualLength = initialLength - inputStream.available();
        return requestLength - actualLength;
    }

    /**
     * Skips remaining request data.
     */
    public void skipRequest() {
        inputStream.skip(getRemainingRequestLength());
    }

    /**
     * @return Input stream for reading requests
     */
    public XInputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return Output stream for writing replies/events
     */
    public XOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Gets event mask for a window.
     * 
     * @param window The window
     * @return Event mask (empty if not listening)
     */
    public Bitmask getEventMaskForWindow(Window window) {
        EventListener eventListener = eventListeners.get(window);
        return eventListener != null ? eventListener.eventMask() : new Bitmask();
    }

    /**
     * Called when a resource is freed externally.
     * Removes from tracking lists.
     * 
     * @param resource The freed resource
     */
    @Override
    public void onFreeResource(XResource resource) {
        if (resource instanceof Window) eventListeners.remove(resource);
        resources.remove(resource);
    }

    /**
     * Validates that a resource ID belongs to this client.
     * 
     * @param id The resource ID to check
     * @return true if owned by this client
     */
    public boolean isValidResourceId(int id) {
        return xServer.resourceIDs.isInInterval(id, resourceIDBase);
    }
}
