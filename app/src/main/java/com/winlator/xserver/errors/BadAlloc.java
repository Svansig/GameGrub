package com.winlator.xserver.errors;

/**
 * BadAlloc - Allocation error.
 * Error code 11: The server failed to allocate the requested resource.
 */
public class BadAlloc extends XRequestError {
    public BadAlloc() {
        super(11, 0);
    }
}
