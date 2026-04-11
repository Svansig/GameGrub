package com.winlator.inputcontrols;

import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a controller button/axis binding for external controllers.
 * Maps Android input events (key codes, axes) to internal binding types.
 */
public class ExternalControllerBinding {
    public static final byte AXIS_X_NEGATIVE = -1;
    public static final byte AXIS_X_POSITIVE = -2;
    public static final byte AXIS_Y_NEGATIVE = -3;
    public static final byte AXIS_Y_POSITIVE = -4;
    public static final byte AXIS_Z_NEGATIVE = -5;
    public static final byte AXIS_Z_POSITIVE = -6;
    public static final byte AXIS_RZ_NEGATIVE = -7;
    public static final byte AXIS_RZ_POSITIVE = -8;
    private short keyCode;
    private Binding binding = Binding.NONE;

    public int getKeyCodeForAxis() {
        return this.keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = (short)keyCode;
    }

    public Binding getBinding() {
        return this.binding;
    }

    public void setBinding(Binding binding) {
        this.binding = binding;
    }

    public JSONObject toJSONObject() {
        try {
            JSONObject controllerBindingJSONObject = new JSONObject();
            controllerBindingJSONObject.put("keyCode", (int) this.keyCode);
            controllerBindingJSONObject.put("binding", this.binding.name());
            return controllerBindingJSONObject;
        } catch (JSONException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        short s = this.keyCode;
        return switch (s) {
            case AXIS_RZ_POSITIVE -> "AXIS RZ+";
            case AXIS_RZ_NEGATIVE -> "AXIS RZ-";
            case AXIS_Z_POSITIVE -> "AXIS Z+";
            case AXIS_Z_NEGATIVE -> "AXIS Z-";
            case AXIS_Y_POSITIVE -> "AXIS Y+";
            case AXIS_Y_NEGATIVE -> "AXIS Y-";
            case AXIS_X_POSITIVE -> "AXIS X+";
            case AXIS_X_NEGATIVE -> "AXIS X-";
            default -> KeyEvent.keyCodeToString(s).replace("KEYCODE_", "").replace("_", " ");
        };
    }

    public static int getKeyCodeForAxis(int axis, byte sign) {
        return switch (axis) {
            case MotionEvent.AXIS_X -> sign > 0 ? AXIS_X_POSITIVE : AXIS_X_NEGATIVE;
            case MotionEvent.AXIS_Y -> sign > 0 ? AXIS_Y_NEGATIVE : AXIS_Y_POSITIVE;
            case MotionEvent.AXIS_Z -> sign > 0 ? AXIS_Z_POSITIVE : AXIS_Z_NEGATIVE;
            case MotionEvent.AXIS_RZ -> sign > 0 ? AXIS_RZ_NEGATIVE : AXIS_RZ_POSITIVE;
            case MotionEvent.AXIS_HAT_X -> sign > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
            case MotionEvent.AXIS_HAT_Y -> sign > 0 ? KeyEvent.KEYCODE_DPAD_DOWN : KeyEvent.KEYCODE_DPAD_UP;
            default -> KeyEvent.KEYCODE_UNKNOWN;
        };
    }
}
