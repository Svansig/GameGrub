package com.winlator.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Utility class for parsing ELF (Executable and Linkable Format) binary headers.
 * Determines whether a binary is 32-bit or 64-bit, used for identifying
 * the architecture of executables within the container.
 */
public abstract class ElfHelper {
    private static final byte ELF_CLASS_32 = 1;
    private static final byte ELF_CLASS_64 = 2;

    private static int getEIClass(File binFile) {
        try (InputStream inStream = new FileInputStream(binFile)) {
            byte[] header = new byte[52];
            inStream.read(header);
            if (header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                return header[4];
            }
        }
        catch (IOException e) {
            Timber.tag("ElfHelper").e(e, "Failed to get EI class");
        }
        return 0;
    }

    public static boolean is32Bit(File binFile) {
        return getEIClass(binFile) == ELF_CLASS_32;
    }

    public static boolean is64Bit(File binFile) {
        return getEIClass(binFile) == ELF_CLASS_64;
    }
}
