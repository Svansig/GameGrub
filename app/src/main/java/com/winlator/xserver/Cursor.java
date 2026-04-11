package com.winlator.xserver;

/**
 * Cursor - X11 cursor object.
 * 
 * Represents a mouse cursor:
 * - hotSpotX/Y: Hotspot position (pointer offset)
 * - cursorImage: Main cursor bitmap
 * - sourceImage: Foreground color image
 * - maskImage: Alpha mask
 * - visible: Visibility state
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Cursors</a>
 */
public class Cursor extends XResource {
    public final int hotSpotX;
    public final int hotSpotY;
    public final Drawable cursorImage;
    public final Drawable sourceImage;
    public final Drawable maskImage;
    private boolean visible = true;

    public Cursor(int id, int hotSpotX, int hotSpotY, Drawable cursorImage, Drawable sourceImage, Drawable maskImage) {
        super(id);
        this.hotSpotX = hotSpotX;
        this.hotSpotY = hotSpotY;
        this.cursorImage = cursorImage;
        this.sourceImage = sourceImage;
        this.maskImage = maskImage;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
