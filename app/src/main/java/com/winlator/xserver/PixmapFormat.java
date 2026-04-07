package com.winlator.xserver;

public record PixmapFormat(byte depth, byte bitsPerPixel, byte scanlinePad) {
    public PixmapFormat(int depth, int bitsPerPixel, int scanlinePad) {
        this.depth = (byte) depth;
        this.bitsPerPixel = (byte) bitsPerPixel;
        this.scanlinePad = (byte) scanlinePad;
    }
}
