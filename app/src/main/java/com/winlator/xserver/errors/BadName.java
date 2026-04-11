package com.winlator.xserver.errors;

/**
 * BadName - Invalid name error.
 * Error code 15: The name does not exist or is invalid.
 */
public class BadName extends XRequestError {
    public BadName() {
        super(15, 0);
    }
}
