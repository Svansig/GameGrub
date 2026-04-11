package com.winlator.xserver.errors;

/**
 * BadIdChoice - Invalid resource ID choice error.
 * Error code 14: The specified resource ID is already in use or out of valid range.
 */
public class BadIdChoice extends XRequestError {
    public BadIdChoice(int id) {
        super(14, id);
    }
}
