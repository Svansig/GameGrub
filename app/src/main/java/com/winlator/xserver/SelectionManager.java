package com.winlator.xserver;

import android.util.SparseArray;

import com.winlator.xserver.events.SelectionClear;

/**
 * SelectionManager - Manages X11 selections (clipboard).
 * 
 * Manages X11 selection ownership:
 * - PRIMARY, SECONDARY, CLIPBOARD
 * - Ownership tracking
 * - SelectionClear events
 * 
 * X11 selections are like clipboard with notifications.
 * When data is copied, selection owner changes.
 * Applications can request selection data.
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Selections</a>
 */
public class SelectionManager implements XResourceManager.OnResourceLifecycleListener {
    private final SparseArray<Selection> selections = new SparseArray<>();

    public SelectionManager(WindowManager windowManager) {
        windowManager.addOnResourceLifecycleListener(this);
    }

    public static class Selection {
        public Window owner;
        private XClient client;
    }

    public void setSelection(int atom, Window owner, XClient client, int timestamp) {
        Selection selection = getSelection(atom);
        if (selection.owner != null && (owner == null || selection.client != client)) {
            selection.client.sendEvent(new SelectionClear(timestamp, owner, atom));
        }
        selection.owner = owner;
        selection.client = client;
    }

    public Selection getSelection(int atom) {
        Selection selection = selections.get(atom);
        if (selection != null) return selection;
        selection = new Selection();
        selections.put(atom, selection);
        return selection;
    }

    @Override
    public void onFreeResource(XResource resource) {
        for (int i = 0; i < selections.size(); i++) {
            Selection selection = selections.valueAt(i);
            if (selection.owner == resource) selection.owner = null;
        }
    }
}
