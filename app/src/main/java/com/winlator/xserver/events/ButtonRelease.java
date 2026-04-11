package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/**
 * X11 event sent when a mouse button is released.
 * Notifies clients that a previously pressed button has been released,
 * enabling click detection and button state tracking.
 */
public class ButtonRelease extends InputDeviceEvent {
    public ButtonRelease(byte detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(5, detail, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
