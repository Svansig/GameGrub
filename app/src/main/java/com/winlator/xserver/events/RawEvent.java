package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;

import java.io.IOException;

/**
 * Wrapper for passing through raw X11 event bytes directly.
 * Used for forwarding unmodified event data to clients without
 * parsing, preserving protocol-level compatibility.
 */
public class RawEvent extends Event {
    private final byte[] data;

    public RawEvent(byte[] data) {
        super(data[0]);
        this.data = data;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.write(data);
        }
    }
}
