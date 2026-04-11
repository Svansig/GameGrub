package com.winlator.xserver;

/**
 * Represents the format of an X11 pixmap, defining color depth, bits per pixel, and scanline padding.
 * Used by the X server to describe supported pixmap formats to clients during connection setup.
 */
public record PixmapFormat(byte depth, byte bitsPerPixel, byte scanlinePad) {
    public PixmapFormat(int depth, int bitsPerPixel, int scanlinePad) {
        this((byte) depth, (byte) bitsPerPixel, (byte) scanlinePad);
    }
}
