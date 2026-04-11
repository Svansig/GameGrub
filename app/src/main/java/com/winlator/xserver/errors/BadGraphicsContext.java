package com.winlator.xserver.errors;

/**
 * BadGraphicsContext - Invalid graphics context ID error.
 * Error code 13: The specified graphics context does not exist.
 */
public class BadGraphicsContext extends XRequestError {
    public BadGraphicsContext(int id) {
        super(13, id);
    }
}
