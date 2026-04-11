package com.winlator.core;

import android.content.res.Resources;

/**
 * Utility class for converting between dp and pixel units.
 * Provides density-independent conversion for UI measurements.
 */
public class UnitUtils {
    public static float dpToPx(float dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }

    public static float pxToDp(float px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }
}
