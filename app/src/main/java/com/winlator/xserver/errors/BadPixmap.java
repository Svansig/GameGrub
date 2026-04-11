package com.winlator.xserver.errors;

/**
 * BadPixmap - Invalid pixmap ID error.
 * Error code 4: The specified pixmap does not exist.
 */
public class BadPixmap extends XRequestError {
    public BadPixmap(int id) {
        super(4, id);
    }
}
