package com.winlator.math;

/**
 * Mathematical utility functions for floating-point and integer operations.
 * Provides common operations such as clamping, rounding, and sign detection.
 */
public abstract class Mathf {
    public static float clamp(float x, float min, float max) {
        return (x < min) ? min : (Math.min(x, max));
    }

    public static int clamp(int x, int min, int max) {
        return (x < min) ? min : (Math.min(x, max));
    }

    public static float roundTo(float x, float step) {
        return (float)(Math.floor(x / step) * step);
    }

    public static float roundTo(float x, float step, boolean roundHalfDown) {
        return (float) ((roundHalfDown ? Math.floor(x / step) : Math.round(x / step)) * step);
    }

    public static int roundPoint(float x) {
        return (int)(x <= 0 ? Math.floor(x) : Math.ceil(x));
    }

    public static byte sign(float x) {
        return (byte)(x < 0 ? -1 : (x > 0 ? 1 : 0));
    }

    public static float lengthSq(float x, float y) {
        return x * x + y * y;
    }
}
