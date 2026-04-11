package com.winlator.xserver.extensions;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

/**
 * Extension - Base interface for X11 protocol extensions.
 *
 * X11 extensions extend the core X11 protocol with additional functionality.
 * Extensions have their own opcode (negative to distinguish from core requests),
 * error codes, and event codes.
 *
 * Common X11 extensions:
 * - MIT-SHM: Shared memory for efficient image transfer
 * - DRI3: Direct Rendering Infrastructure
 * - Present: Buffer presentation
 * - Sync: Synchronization primitives
 * - BigReq: Extended request length
 *
 * @see <a href="https://www.x.org/wiki/Extensions/">X11 Extension Specification</a>
 */
public interface Extension {
    /**
     * @return Extension name (e.g., "MIT-SHM", "Present", "DRI3")
     */
    String getName();

    /**
     * @return Major opcode for extension requests (always negative)
     */
    byte getMajorOpcode();

    /**
     * @return First error code for this extension
     */
    byte getFirstErrorId();

    /**
     * @return First event code for this extension (0 if no events)
     */
    byte getFirstEventId();

    /**
     * Handles an extension request.
     *
     * @param client The client making the request
     * @param inputStream Stream to read request data
     * @param outputStream Stream to write reply
     * @throws IOException On I/O errors
     * @throws XRequestError On protocol errors
     */
    void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
