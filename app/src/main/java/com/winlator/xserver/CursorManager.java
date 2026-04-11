package com.winlator.xserver;

import android.util.SparseArray;

import java.nio.IntBuffer;

/**
 * CursorManager - Manages X11 cursors.
 * 
 * Manages cursor objects:
 * - Creates/destroys cursors
 * - Tracks cursors by ID
 * - Masks for cursor shape
 * - Hotspot position
 * 
 * Cursors define the mouse pointer appearance.
 * Can use custom bitmaps or system cursors.
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Cursors</a>
 */
public class CursorManager extends XResourceManager {
    private final SparseArray<Cursor> cursors = new SparseArray<>();
    private final DrawableManager drawableManager;

    public CursorManager(DrawableManager drawableManager) {
        this.drawableManager = drawableManager;
    }

    public Cursor getCursor(int id) {
        return cursors.get(id);
    }

    public Cursor createCursor(int id, short x, short y, Pixmap sourcePixmap, Pixmap maskPixmap) {
        if (cursors.indexOfKey(id) >= 0) return null;
        Drawable drawable = drawableManager.createDrawable(0, sourcePixmap.drawable.width, sourcePixmap.drawable.height, sourcePixmap.drawable.visual);
        Cursor cursor = new Cursor(id, x, y, drawable, sourcePixmap.drawable, maskPixmap != null ? maskPixmap.drawable : null);
        cursors.put(id, cursor);
        triggerOnCreateResourceListener(cursor);
        return cursor;
    }

    public void freeCursor(int id) {
        triggerOnFreeResourceListener(cursors.get(id));
        cursors.remove(id);
    }

    private static boolean isEmptyMaskImage(Drawable maskImage) {
        IntBuffer maskData = maskImage.getData().asIntBuffer();
        boolean result = true;
        for (int i = 0; i < maskData.capacity(); i++) {
            if (maskData.get(i) != 0x000000) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void recolorCursor(Cursor cursor, byte foreRed, byte foreGreen, byte foreBlue, byte backRed, byte backGreen, byte backBlue) {
        if (cursor.maskImage != null) {
            boolean visible = !isEmptyMaskImage(cursor.maskImage);
            cursor.setVisible(visible);
            if (visible) cursor.cursorImage.drawAlphaMaskedBitmap(foreRed, foreGreen, foreBlue, backRed, backGreen, backBlue, cursor.sourceImage, cursor.maskImage);
        }
    }
}