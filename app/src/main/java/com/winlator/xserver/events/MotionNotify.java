package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/**
 * X11 event sent when the pointer moves within a window.
 * Notifies clients of cursor position changes for mouse tracking,
 * enabling UI element highlighting and coordinate-based interactions.
 */
public class MotionNotify extends InputDeviceEvent {
    public MotionNotify(boolean detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state) {
        super(6, (byte)(detail ? 1 : 0), root, event, child, rootX, rootY, eventX, eventY, state);
    }
}
