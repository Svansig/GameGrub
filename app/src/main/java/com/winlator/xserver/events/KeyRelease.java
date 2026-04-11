package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/**
 * X11 event sent when a key is released.
 * Notifies clients that a previously pressed key has been released,
 * enabling key state tracking for keyboard input handling.
 */
public class KeyRelease extends InputDeviceEvent {
    public KeyRelease(byte keycode, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(3, keycode, root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
