package com.winlator.xserver;

import android.util.SparseArray;

import com.winlator.core.Callback;
import com.winlator.renderer.Texture;

/**
 * DrawableManager - Manages X11 drawable surfaces.
 * 
 * Manages all Drawables (Windows and Pixmaps share this):
 * - Creates drawable surfaces
 * - Tracks active drawables by ID
 * - Coordinates with PixmapManager
 * 
 * Both Windows and Pixmaps use Drawable for their content.
 * The DrawableManager is the central registry.
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Drawables</a>
 */
public class DrawableManager extends XResourceManager implements XResourceManager.OnResourceLifecycleListener {
    private final XServer xServer;
    private final SparseArray<Drawable> drawables = new SparseArray<>();

    public DrawableManager(XServer xServer) {
        this.xServer = xServer;
        xServer.pixmapManager.addOnResourceLifecycleListener(this);
    }

    public Drawable getDrawable(int id) {
        return drawables.get(id);
    }

    public Drawable createDrawable(int id, short width, short height, byte depth) {
        return createDrawable(id, width, height, xServer.pixmapManager.getVisualForDepth(depth));
    }

    public Drawable createDrawable(int id, short width, short height, Visual visual) {
        if (id == 0) return new Drawable(id, width, height, visual);
        if (drawables.indexOfKey(id) >= 0) return null;
        Drawable drawable = new Drawable(id, width, height, visual);
        drawables.put(id, drawable);
        return drawable;
    }

    public void removeDrawable(int id) {
        Drawable drawable = drawables.get(id);

        final Texture texture = drawable.getTexture();
//        if (texture != null) {
//            XServerView xServerView = this.xServer.getRenderer().xServerView;
//            Objects.requireNonNull(texture);
//            xServerView.queueEvent(() -> VortekRendererComponent.destroyTexture(texture));
//        }
        if (texture != null) xServer.getRenderer().xServerView.queueEvent(texture::destroy);

        Callback<Drawable> onDestroyListener = drawable.getOnDestroyListener();
        if (onDestroyListener != null) onDestroyListener.call(drawable);

        drawable.setOnDrawListener(null);
        drawables.remove(id);
    }

    @Override
    public void onFreeResource(XResource resource) {
        if (resource instanceof Pixmap) removeDrawable(((Pixmap)resource).drawable.id);
    }

    public Visual getVisual() {
        return xServer.pixmapManager.visual;
    }

    public SparseArray<Drawable> all(){
        return drawables;
    }
}
