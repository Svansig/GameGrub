package com.winlator.xserver;

/**
 * Simple global ID generator for X server resources.
 * Provides sequential unique IDs for internal X server
 * object identification.
 */
public abstract class IDGenerator {
    private static int id = 0;

    public static int generate() {
        return ++id;
    }
}
