package com.winlator.xserver.errors;

/**
 * BadFence - Invalid fence ID error.
 * Error code: Extension-specific (DRI3).
 */
public class BadFence extends XRequestError {
    public BadFence(int id) {
        super(Byte.MIN_VALUE + 2, id);
    }
}
