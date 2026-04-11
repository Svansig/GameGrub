package com.winlator.xserver.errors;

/**
 * BadAccess - Access error.
 * Error code 10: The client attempted to access a resource that is not owned
 * or is not accessible due to ownership or permission constraints.
 */
public class BadAccess extends XRequestError {
    public BadAccess() {
        super(10, 0);
    }
}
