package com.winlator.xserver;

/**
 * Auto-closeable lock interface for X server resource locking.
 * Used to synchronize access to X11 resources during request handling,
 * ensuring thread-safe operations on shared server state.
 */
public interface XLock extends AutoCloseable {
    @Override
    void close();
}
