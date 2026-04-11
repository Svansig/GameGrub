package com.winlator.xserver.errors;

/**
 * BadImplementation - Implementation error.
 * Error code 17: The server does not implement the requested functionality.
 */
public class BadImplementation extends XRequestError {
    public BadImplementation() {
        super(17, 0);
    }
}
