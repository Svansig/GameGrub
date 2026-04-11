package com.winlator.xserver;

import androidx.annotation.NonNull;

/**
 * X11 screen information container for display dimensions and DPI.
 * Stores screen width/height in pixels and physical dimensions
 * for display configuration and resolution reporting.
 */
public class ScreenInfo {
    public final short width;
    public final short height;

    public ScreenInfo(String value) {
        String[] parts = value.split("x");
        width = Short.parseShort(parts[0]);
        height = Short.parseShort(parts[1]);
    }

    public ScreenInfo(int width, int height) {
        this.width = (short)width;
        this.height = (short)height;
    }

    public short getWidthInMillimeters() {
        return (short)(width / 10);
    }

    public short getHeightInMillimeters() {
        return (short)(height / 10);
    }

    @NonNull
    @Override
    public String toString() {
        return width+"x"+height;
    }
}
