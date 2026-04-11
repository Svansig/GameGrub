package com.winlator.xconnector;

import java.io.IOException;

/**
 * Interface for acquiring exclusive locks on X11 stream operations. Implementations ensure
 * thread-safe access to input/output streams during protocol message processing.
 */
public interface XStreamLock extends AutoCloseable {
    void close() throws IOException;
}
