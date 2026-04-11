package com.winlator.core;

import java.io.File;

/**
 * Listener interface for file extraction progress events.
 * Allows modification of destination file during extraction.
 */
public interface OnExtractFileListener {
    File onExtractFile(File destination, long size);
}
