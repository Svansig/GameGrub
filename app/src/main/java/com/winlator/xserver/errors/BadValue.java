package com.winlator.xserver.errors;

/**
 * BadValue - Invalid value error.
 * Error code 2: The value for an argument is out of range.
 */
public class BadValue extends XRequestError {
    public BadValue(int data) {
        super(2, data);
    }
}
