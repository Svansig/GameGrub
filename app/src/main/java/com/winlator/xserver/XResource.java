package com.winlator.xserver;

/**
 * Base class for all X11 resources (windows, drawables, pixmaps, etc).
 * Provides common ID-based identification used throughout the X server
 * for referencing and managing server-side graphical resources.
 */
public abstract class XResource {
    public final int id;

    public XResource(int id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
