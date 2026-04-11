package com.winlator.xserver;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

/**
 * Represents an X11 pixmap, an off-screen image buffer that can be drawn to and used as a source
 * for image operations. Pixmaps are managed resources identified by XID and can be converted to
 * Android Bitmaps for rendering to the screen.
 */
public class Pixmap extends XResource {
    public final Drawable drawable;

    public Pixmap(Drawable drawable) {
        super(drawable.id);
        this.drawable = drawable;
    }

    public Bitmap toBitmap(Pixmap maskPixmap) {
        ByteBuffer maskData = maskPixmap != null ? maskPixmap.drawable.getData() : null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.width, drawable.height, Bitmap.Config.ARGB_8888);
        toBitmap(drawable.getData(), maskData, bitmap);
        return bitmap;
    }

    private static native void toBitmap(ByteBuffer colorData, ByteBuffer maskData, Bitmap bitmap);
}
