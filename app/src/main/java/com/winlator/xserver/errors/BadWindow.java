package com.winlator.xserver.errors;

/**
 * BadWindow - Invalid window ID error.
 * Error code 3: The specified window does not exist.
 * Sent when a request references a non-existent window.
 * @see <a href="https://www.x.org/wiki/X11/">X11 Errors</a>
 */
public class BadWindow extends XRequestError {
    public BadWindow(int id) {
        super(3, id);
    }
}
