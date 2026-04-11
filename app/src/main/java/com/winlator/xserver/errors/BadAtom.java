package com.winlator.xserver.errors;

/**
 * BadAtom - Invalid atom ID error.
 * Error code 5: The specified atom does not exist.
 */
public class BadAtom extends XRequestError {
    public BadAtom(int id) {
        super(5, id);
    }
}
