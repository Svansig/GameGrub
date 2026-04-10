package com.winlator.xserver.extensions;

import android.util.SparseBooleanArray;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadFence;
import com.winlator.xserver.errors.BadIdChoice;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

import timber.log.Timber;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

/**
 * X11 Synchronization (SYNC) Extension implementation.
 * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/syncproto.txt">SYNC Protocol Specification</a>
 */
public class SyncExtension implements Extension {
    public static final byte MAJOR_OPCODE = -104;
    private final SparseBooleanArray fences = new SparseBooleanArray();

    /**
     * SYNC extension request opcodes.
     * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/syncproto.txt">SYNC Protocol - Requests</a>
     */
    private static abstract class ClientOpcodes {
        /**
         * SyncInitialize (opcode 0) - Initialize the SYNC extension.
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/syncproto.txt">SyncInitialize</a>
         */
        private static final byte INITIALIZE = 0;
        private static final byte CREATE_FENCE = 14;
        private static final byte TRIGGER_FENCE = 15;
        private static final byte RESET_FENCE = 16;
        private static final byte DESTROY_FENCE = 17;
        /**
         * SyncQueryFence (opcode 18) - Query the current state of a fence.
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/syncproto.txt">SyncQueryFence</a>
         */
        private static final byte QUERY_FENCE = 18;
        private static final byte AWAIT_FENCE = 19;
    }

    @Override
    public String getName() {
        return "SYNC";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public byte getFirstErrorId() {
        return Byte.MIN_VALUE;
    }

    @Override
    public byte getFirstEventId() {
        return 0;
    }

    public void setTriggered(int id) {
        synchronized (fences) {
            if (fences.indexOfKey(id) >= 0) fences.put(id, true);
        }
    }

    /** Register a fence from DRI3FenceFromFD without reading from a stream. */
    public void createFence(int id, boolean initiallyTriggered) throws com.winlator.xserver.errors.BadIdChoice {
        synchronized (fences) {
            if (fences.indexOfKey(id) >= 0) throw new com.winlator.xserver.errors.BadIdChoice(id);
            fences.put(id, initiallyTriggered);
        }
    }

    public boolean hasFence(int id) {
        synchronized (fences) {
            return fences.indexOfKey(id) >= 0;
        }
    }

    private void initialize(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 3);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(1);
            outputStream.writePad(20);
        }
    }

    private void createFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (fences) {
            inputStream.skip(4);
            int id = inputStream.readInt();

            if (fences.indexOfKey(id) >= 0) throw new BadIdChoice(id);

            boolean initiallyTriggered = inputStream.readByte() == 1;
            inputStream.skip(3);

            fences.put(id, initiallyTriggered);
        }
    }

    private void triggerFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (fences) {
            int id = inputStream.readInt();
            if (fences.indexOfKey(id) < 0) throw new BadFence(id);
            fences.put(id, true);
        }
    }

    private void resetFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (fences) {
            int id = inputStream.readInt();
            if (fences.indexOfKey(id) < 0) throw new BadFence(id);

            boolean triggered = fences.get(id);
            if (!triggered) throw new BadMatch();

            fences.put(id, false);
        }
    }

    private void destroyFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        synchronized (fences) {
            int id = inputStream.readInt();
            if (fences.indexOfKey(id) < 0) throw new BadFence(id);
            fences.delete(id);
        }
    }

    private void queryFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int id = inputStream.readInt();

        boolean triggered;
        synchronized (fences) {
            if (fences.indexOfKey(id) < 0) throw new BadFence(id);
            triggered = fences.get(id);
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeLong(triggered ? 1 : 0);
            outputStream.writePad(16);
        }
    }

    private void awaitFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int length = client.getRemainingRequestLength();
        int[] ids = new int[length / 4];
        int i = 0;

        while (length != 0) {
            ids[i++] = inputStream.readInt();
            length -= 4;
        }

        boolean anyTriggered;
        synchronized (fences) {
            anyTriggered = false;
            for (int id : ids) {
                if (fences.indexOfKey(id) < 0) throw new BadFence(id);
                if (fences.get(id)) {
                    anyTriggered = true;
                    break;
                }
            }
        }

        if (!anyTriggered) {
            Timber.w("[SyncExtension] Await on untriggered fence not supported");
            throw new BadImplementation();
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        switch (opcode) {
            case ClientOpcodes.INITIALIZE:
                initialize(client, inputStream, outputStream);
                break;
            case ClientOpcodes.CREATE_FENCE :
                createFence(client, inputStream, outputStream);
                break;
            case ClientOpcodes.TRIGGER_FENCE:
                triggerFence(client, inputStream, outputStream);
                break;
            case ClientOpcodes.RESET_FENCE:
                resetFence(client, inputStream, outputStream);
                break;
            case ClientOpcodes.DESTROY_FENCE:
                destroyFence(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_FENCE:
                queryFence(client, inputStream, outputStream);
                break;
            case ClientOpcodes.AWAIT_FENCE:
                awaitFence(client, inputStream, outputStream);
                break;
            default:
                Timber.w("[SyncExtension] Unimplemented opcode: %d", opcode);
                throw new BadImplementation();
        }
    }
}
