package com.winlator.xserver.errors;

/**
 * BadMatch - Match error.
 * Error code 8: The argument does not match the expected type or format.
 */
public class BadMatch extends XRequestError {
    public BadMatch() {
        super(8, 0);
    }
}
