package com.winlator.xserver.errors;

public class BadRequest extends XRequestError {
    public BadRequest(int data) {
        super(1, data);
    }
}
