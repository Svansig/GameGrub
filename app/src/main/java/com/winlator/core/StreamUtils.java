package com.winlator.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for stream operations including copying and buffering.
 * Provides helper methods for I/O operations used throughout the application.
 */
public class StreamUtils {
    public static final int BUFFER_SIZE = 64 * 1024;

    public static byte[] copyToByteArray(InputStream inStream) {
        if (inStream == null) return new byte[0];

        ByteArrayOutputStream outStream = new ByteArrayOutputStream(BUFFER_SIZE);
        copy(inStream, outStream);
        return outStream.toByteArray();
    }

    public static boolean copy(InputStream inStream, OutputStream outStream) {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int amountRead;
            while ((amountRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, amountRead);
            }
            outStream.flush();
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
