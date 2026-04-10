package com.winlator.xserver.errors;

/** X11 error code 15 — the name does not exist or is invalid. */
public class BadName extends XRequestError {
    public BadName() {
        super(15, 0);
    }
}
