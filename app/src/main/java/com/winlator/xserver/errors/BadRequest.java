package com.winlator.xserver.errors;

/**
 * BadRequest - Invalid request error.
 * Error code 1: The request does not include the correct request length or format.
 */
public class BadRequest extends XRequestError {
    public BadRequest(int data) {
        super(1, data);
    }
}
