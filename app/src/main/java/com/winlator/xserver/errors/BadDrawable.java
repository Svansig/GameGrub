package com.winlator.xserver.errors;

/**
 * BadDrawable - Invalid drawable ID error.
 * Error code 9: The specified drawable does not exist.
 * Sent when a request references a non-existent drawable.
 * @see <a href="https://www.x.org/wiki/X11/">X11 Errors</a>
 */
public class BadDrawable extends XRequestError {
    public BadDrawable(int id) {
        super(9, id);
    }
}
