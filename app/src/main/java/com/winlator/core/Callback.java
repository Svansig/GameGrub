package com.winlator.core;

/**
 * Generic callback interface for asynchronous operations.
 * @param <T> The type of object passed to the callback
 */
public interface Callback<T> {
    void call(T object);
}
