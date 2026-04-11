package com.winlator.winhandler;

import com.winlator.xserver.Pointer;

/**
 * Constants for mouse event flags used in Windows mouse input events.
 * Maps X server pointer buttons to Windows mouse event flags.
 */
public abstract class MouseEventFlags {
    public static final int MOVE = 0x0001;
    public static final int LEFTDOWN = 0x0002;
    public static final int LEFTUP = 0x0004;
    public static final int RIGHTDOWN = 0x0008;
    public static final int RIGHTUP = 0x0010;
    public static final int MIDDLEDOWN = 0x0020;
    public static final int MIDDLEUP = 0x0040;
    public static final int XDOWN = 0x0080;
    public static final int XUP = 0x0100;
    public static final int WHEEL = 0x0800;
    public static final int VIRTUALDESK = 0x4000;
    public static final int ABSOLUTE = 0x8000;

    public static int getFlagFor(Pointer.Button button, boolean isActionDown) {
        return switch (button) {
            case BUTTON_LEFT -> isActionDown ? MouseEventFlags.LEFTDOWN : MouseEventFlags.LEFTUP;
            case BUTTON_MIDDLE -> isActionDown ? MouseEventFlags.MIDDLEDOWN : MouseEventFlags.MIDDLEUP;
            case BUTTON_RIGHT -> isActionDown ? MouseEventFlags.RIGHTDOWN : MouseEventFlags.RIGHTUP;
            case BUTTON_SCROLL_DOWN, BUTTON_SCROLL_UP -> MouseEventFlags.WHEEL;
            default -> 0;
        };
    }
}
