package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/**
 * X11 event sent when the pointer leaves a window or grab area.
 * Notifies clients that the cursor has exited their window region,
 * enabling hover state changes and cursor tracking updates.
 */
public class LeaveNotify extends PointerWindowEvent {
    public LeaveNotify(Detail detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state, Mode mode, boolean sameScreenAndFocus) {
        super(8, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus);
    }
}
