package com.winlator.winhandler;

/**
 * Listener interface for receiving process information during enumeration.
 */
public interface OnGetProcessInfoListener {
    void onGetProcessInfo(int index, int count, ProcessInfo processInfo);
}
