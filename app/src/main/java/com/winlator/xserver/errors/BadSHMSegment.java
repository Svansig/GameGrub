package com.winlator.xserver.errors;

/**
 * BadSHMSegment - Invalid shared memory segment error.
 * Error code: MIT-SHM extension specific.
 */
public class BadSHMSegment extends XRequestError {
    public BadSHMSegment(int id) {
        super(-128, id);
    }
}
