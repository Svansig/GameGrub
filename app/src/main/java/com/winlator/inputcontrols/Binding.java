package com.winlator.inputcontrols;

import androidx.annotation.NonNull;

import com.winlator.xserver.Pointer;
import com.winlator.xserver.XKeycode;

import java.util.ArrayList;

public enum Binding {
    NONE, MOUSE_LEFT_BUTTON, MOUSE_MIDDLE_BUTTON, MOUSE_RIGHT_BUTTON, MOUSE_MOVE_LEFT, MOUSE_MOVE_RIGHT, MOUSE_MOVE_UP, MOUSE_MOVE_DOWN, MOUSE_SCROLL_UP, MOUSE_SCROLL_DOWN, KEY_UP, KEY_RIGHT, KEY_DOWN, KEY_LEFT, KEY_ENTER, KEY_ESC, KEY_BKSP, KEY_DEL, KEY_TAB, KEY_SPACE, KEY_CTRL_L, KEY_CTRL_R, KEY_SHIFT_L, KEY_SHIFT_R, KEY_ALT_L, KEY_ALT_R, KEY_HOME, KEY_PRTSCN, KEY_PG_UP, KEY_PG_DOWN, KEY_CAPS_LOCK, KEY_NUM_LOCK, KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J, KEY_K, KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T, KEY_U, KEY_V, KEY_W, KEY_X, KEY_Y, KEY_Z, KEY_BRACKET_LEFT, KEY_BRACKET_RIGHT, KEY_BACKSLASH, KEY_SLASH, KEY_SEMICOLON, KEY_COMMA, KEY_PERIOD, KEY_APOSTROPHE, KEY_KP_ADD, KEY_MINUS, KEY_F1, KEY_F2, KEY_F3, KEY_F4, KEY_F5, KEY_F6, KEY_F7, KEY_F8, KEY_F9, KEY_F10, KEY_F11, KEY_F12, KEY_KP_0, KEY_KP_1, KEY_KP_2, KEY_KP_3, KEY_KP_4, KEY_KP_5, KEY_KP_6, KEY_KP_7, KEY_KP_8, KEY_KP_9, OPEN_NAVIGATION_MENU, GAMEPAD_BUTTON_A, GAMEPAD_BUTTON_B, GAMEPAD_BUTTON_X, GAMEPAD_BUTTON_Y, GAMEPAD_BUTTON_L1, GAMEPAD_BUTTON_R1, GAMEPAD_BUTTON_SELECT, GAMEPAD_BUTTON_START, GAMEPAD_BUTTON_L3, GAMEPAD_BUTTON_R3, GAMEPAD_BUTTON_L2, GAMEPAD_BUTTON_R2, GAMEPAD_LEFT_THUMB_UP, GAMEPAD_LEFT_THUMB_RIGHT, GAMEPAD_LEFT_THUMB_DOWN, GAMEPAD_LEFT_THUMB_LEFT, GAMEPAD_RIGHT_THUMB_UP, GAMEPAD_RIGHT_THUMB_RIGHT, GAMEPAD_RIGHT_THUMB_DOWN, GAMEPAD_RIGHT_THUMB_LEFT, GAMEPAD_DPAD_UP, GAMEPAD_DPAD_RIGHT, GAMEPAD_DPAD_DOWN, GAMEPAD_DPAD_LEFT;
    public final XKeycode keycode;

    Binding() {
        XKeycode keycode;
        try {
            keycode = XKeycode.valueOf(name());
        }
        catch (IllegalArgumentException e) {
            keycode = XKeycode.KEY_NONE;
            String name = name();
            if (name.equals("KEY_PG_UP")) {
                keycode = XKeycode.KEY_PRIOR;
            }
            else if (name.equals("KEY_PG_DOWN")) {
                keycode = XKeycode.KEY_NEXT;
            }
        }
        this.keycode = keycode;
    }

    @NonNull
    @Override
    public String toString() {
        return switch (this) {
            case KEY_SHIFT_L -> "L SHIFT";
            case KEY_SHIFT_R -> "R SHIFT";
            case KEY_CTRL_L -> "L CTRL";
            case KEY_CTRL_R -> "R CTRL";
            case KEY_ALT_L -> "L ALT";
            case KEY_ALT_R -> "R ALT";
            case KEY_BRACKET_LEFT -> "[";
            case KEY_BRACKET_RIGHT -> "]";
            case KEY_BACKSLASH -> "\\";
            case KEY_SLASH -> "/";
            case KEY_SEMICOLON -> ";";
            case KEY_COMMA -> ",";
            case KEY_PERIOD -> ".";
            case KEY_APOSTROPHE -> "'";
            case KEY_MINUS -> "-";
            case KEY_KP_ADD -> "+";
            default -> super.toString().replaceAll("^(MOUSE_)|(KEY_)|(GAMEPAD_)", "").replace("KP_", "NUMPAD_").replace("_", " ");
        };
    }

    public static Binding fromString(String name) {
        return switch (name) {
            case "KEY_CTRL" -> Binding.KEY_CTRL_L;
            case "KEY_SHIFT" -> Binding.KEY_SHIFT_L;
            case "KEY_ALT" -> Binding.KEY_ALT_L;
            default -> valueOf(name);
        };
    }

    public Pointer.Button getPointerButton() {
        return switch (this) {
            case MOUSE_LEFT_BUTTON -> Pointer.Button.BUTTON_LEFT;
            case MOUSE_MIDDLE_BUTTON -> Pointer.Button.BUTTON_MIDDLE;
            case MOUSE_RIGHT_BUTTON -> Pointer.Button.BUTTON_RIGHT;
            case MOUSE_SCROLL_UP -> Pointer.Button.BUTTON_SCROLL_UP;
            case MOUSE_SCROLL_DOWN -> Pointer.Button.BUTTON_SCROLL_DOWN;
            default -> null;
        };
    }

    public boolean isMouse() {
        return name().startsWith("MOUSE_");
    }

    public boolean isKeyboard() {
        return name().startsWith("KEY_") || this == NONE;
    }

    public boolean isGamepad() {
        return name().startsWith("GAMEPAD_");
    }

    public boolean isExtra() {
        return this == OPEN_NAVIGATION_MENU;
    }

    public boolean isMouseMove() {
        return this == MOUSE_MOVE_UP || this == MOUSE_MOVE_RIGHT || this == MOUSE_MOVE_DOWN || this == MOUSE_MOVE_LEFT;
    }

    public static String[] mouseBindingLabels() {
        ArrayList<String> names = new ArrayList<>();
        for (Binding binding : values()) if (binding.isMouse()) names.add(binding.toString());
        return names.toArray(new String[0]);
    }

    public static String[] keyboardBindingLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Binding binding : values()) if (binding.isKeyboard()) labels.add(binding.toString());
        return labels.toArray(new String[0]);
    }

    public static String[] gamepadBindingLabels() {
        ArrayList<String> names = new ArrayList<>();
        for (Binding binding : values()) if (binding.isGamepad()) names.add(binding.toString());
        return names.toArray(new String[0]);
    }

    public static Binding[] mouseBindingValues() {
        ArrayList<Binding> labels = new ArrayList<>();
        for (Binding binding : values()) if (binding.isMouse()) labels.add(binding);
        return labels.toArray(new Binding[0]);
    }

    public static Binding[] keyboardBindingValues() {
        ArrayList<Binding> values = new ArrayList<>();
        for (Binding binding : values()) if (binding.isKeyboard()) values.add(binding);
        return values.toArray(new Binding[0]);
    }

    public static Binding[] gamepadBindingValues() {
        ArrayList<Binding> labels = new ArrayList<>();
        for (Binding binding : values()) if (binding.isGamepad()) labels.add(binding);
        return labels.toArray(new Binding[0]);
    }

    public static String[] extraBindingLabels() {
        ArrayList<String> labels = new ArrayList<>();
        for (Binding binding : values()) if (binding.isExtra()) labels.add(binding.toString());
        return labels.toArray(new String[0]);
    }

    public static Binding[] extraBindingValues() {
        ArrayList<Binding> values = new ArrayList<>();
        for (Binding binding : values()) if (binding.isExtra()) values.add(binding);
        return values.toArray(new Binding[0]);
    }
}
