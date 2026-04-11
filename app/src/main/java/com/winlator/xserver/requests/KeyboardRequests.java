package com.winlator.xserver.requests;

import static com.winlator.xserver.Keyboard.KEYSYMS_PER_KEYCODE;
import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Keyboard;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XKeycode;
import com.winlator.xserver.errors.BadValue;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

/**
 * X server request handlers for keyboard operations.
 * Handles QueryKeymap, GetKeyboardMapping, ChangeKeyboardMapping,
 * GetKeyboardControl, and GetModifierMapping requests.
 */
public abstract class KeyboardRequests {
    public static void queryKeymap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        byte[] keymap = new byte[32];
        for (Byte keycode : client.xServer.keyboard.getPressedKeys()) {
            int code = Byte.toUnsignedInt(keycode);
            if (code < 256) keymap[code / 8] |= (byte)(1 << (code % 8));
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(2);
            outputStream.write(keymap);
        }
    }

    public static void getKeyboardMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        byte firstKeycode = inputStream.readByte();
        int count = inputStream.readUnsignedByte();
        inputStream.skip(2);

        int first = Byte.toUnsignedInt(firstKeycode);
        if (first < Keyboard.MIN_KEYCODE || count == 0 || (first + count - 1) > Keyboard.MAX_KEYCODE) {
            throw new BadValue(first);
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(KEYSYMS_PER_KEYCODE);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(count * KEYSYMS_PER_KEYCODE);
            outputStream.writePad(24);

            int i = first - Keyboard.MIN_KEYCODE;
            int remaining = count;
            while (remaining != 0) {
                for (int k = 0; k < KEYSYMS_PER_KEYCODE; k++) {
                    outputStream.writeInt(client.xServer.keyboard.keysyms[i * KEYSYMS_PER_KEYCODE + k]);
                }
                remaining--;
                i++;
            }
        }
    }

    public static void changeKeyboardMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int keycodeCount = Byte.toUnsignedInt(client.getRequestData());
        byte firstKeycode = inputStream.readByte();
        int keysymsPerKeycode = Byte.toUnsignedInt(inputStream.readByte());
        inputStream.skip(2);

        int first = Byte.toUnsignedInt(firstKeycode);
        if (first < Keyboard.MIN_KEYCODE || (first + keycodeCount - 1) > Keyboard.MAX_KEYCODE || keysymsPerKeycode == 0) {
            // Skip remaining and ignore — malformed request
            client.skipRequest();
            return;
        }

        // Update keysyms, mapping only the first KEYSYMS_PER_KEYCODE per keycode
        for (int i = 0; i < keycodeCount; i++) {
            int minKeysym = 0, majKeysym = 0;
            for (int k = 0; k < keysymsPerKeycode; k++) {
                int keysym = inputStream.readInt();
                if (k == 0) minKeysym = keysym;
                if (k == 1) majKeysym = keysym;
            }
            client.xServer.keyboard.setKeysyms((byte)(firstKeycode + i), minKeysym, majKeysym);
        }
    }

    public static void getKeyboardControl(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)1); // global-auto-repeat = on
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(5); // reply length = 5 CARD32s (20 bytes for auto-repeats)
            outputStream.writeInt(0); // led-mask
            outputStream.writeByte((byte)0); // key-click-percent
            outputStream.writeByte((byte)50); // bell-percent
            outputStream.writeShort((short)400); // bell-pitch Hz
            outputStream.writeShort((short)100); // bell-duration ms
            outputStream.writePad(2);
            outputStream.writePad(32); // auto-repeats bitmap (all keys auto-repeat)
        }
    }

    public static void getModifierMapping(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        // 8 modifier slots, 2 keycodes each (keycodes_per_modifier = 2)
        byte kpm = 2;
        byte[] mapping = {
            XKeycode.KEY_SHIFT_L.getId(), XKeycode.KEY_SHIFT_R.getId(), // Shift
            XKeycode.KEY_CAPS_LOCK.getId(), 0,                          // Lock
            XKeycode.KEY_CTRL_L.getId(), XKeycode.KEY_CTRL_R.getId(),  // Control
            XKeycode.KEY_ALT_L.getId(), XKeycode.KEY_ALT_R.getId(),    // Mod1 (Alt)
            XKeycode.KEY_NUM_LOCK.getId(), 0,                           // Mod2 (NumLock)
            0, 0,                                                        // Mod3
            0, 0,                                                        // Mod4
            0, 0                                                         // Mod5
        };

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(kpm);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(mapping.length / 4);
            outputStream.writePad(24);
            outputStream.write(mapping);
        }
    }
}
