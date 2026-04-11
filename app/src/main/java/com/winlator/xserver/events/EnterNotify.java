package com.winlator.xserver.events;

import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Window;

/**
 * X11 event sent when the pointer enters a window or grab area.
 * Notifies clients that the cursor has entered their window region,
 * enabling hover state activation and focus tracking.
 */
public class EnterNotify extends PointerWindowEvent {
    public EnterNotify(Detail detail, Window root, Window event, Window child, short rootX, short rootY, short eventX, short eventY, Bitmask state, Mode mode, boolean sameScreenAndFocus) {
        super(7, detail, root, event, child, rootX, rootY, eventX, eventY, state, mode, sameScreenAndFocus);
    }
}
