package com.winlator.xserver;

public record PixmapFormat(byte depth, byte bitsPerPixel, byte scanlinePad) {
    public PixmapFormat(int depth, int bitsPerPixel, int scanlinePad) {
        this((byte) depth, (byte) bitsPerPixel, (byte) scanlinePad);
    }
}
