package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import android.util.SparseArray;

import com.winlator.renderer.GPUImage;
import com.winlator.renderer.Texture;
import com.winlator.widget.XServerView;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xenvironment.components.VortekRendererComponent;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Pixmap;
import com.winlator.xserver.Window;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.BadPixmap;
import com.winlator.xserver.errors.BadWindow;
import com.winlator.xserver.errors.XRequestError;
import com.winlator.xserver.events.PresentCompleteNotify;
import com.winlator.xserver.events.PresentIdleNotify;

import java.io.IOException;
import java.util.Objects;

import timber.log.Timber;

/**
 * X11 Present Extension implementation.
 * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/presentproto.txt">Present Protocol Specification</a>
 */
public class PresentExtension implements Extension {
    public static final byte MAJOR_OPCODE = -103;
    private static final int FAKE_INTERVAL = 1000000 / 60;
    public enum Kind {PIXMAP, MSC_NOTIFY}
    public enum Mode {COPY, FLIP, SKIP}

    /**
     * PresentCapability values per spec.
     * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/presentproto.txt">Present Protocol - Data Types</a>
     */
    private static final int CAPABILITY_ASYNC = 1;
    private static final int CAPABILITY_FENCE = 2;
    private static final int CAPABILITY_UST = 4;

    private final SparseArray<Event> events = new SparseArray<>();
    private SyncExtension syncExtension;

    /**
     * Present extension request opcodes.
     * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/presentproto.txt">Present Protocol - Extension Requests</a>
     */
    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte PRESENT_PIXMAP = 1;
        /**
         * PresentNotifyMSC (opcode 2) - Queries the Media Stream Counter for frame timing.
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/presentproto.txt">PresentNotifyMSC</a>
         */
        private static final byte NOTIFY_MSC = 2;
        private static final byte SELECT_INPUT = 3;
        /**
         * PresentQueryCapabilities (opcode 4) - Queries supported Present extension capabilities.
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/presentproto.txt">PresentQueryCapabilities</a>
         */
        private static final byte QUERY_CAPABILITIES = 4;
    }

    private static class Event {
        private Window window;
        private XClient client;
        private int id;
        private Bitmask mask;
    }

    @Override
    public String getName() {
        return "Present";
    }

    @Override
    public byte getMajorOpcode() {
        return MAJOR_OPCODE;
    }

    @Override
    public byte getFirstErrorId() {
        return 0;
    }

    @Override
    public byte getFirstEventId() {
        return 0;
    }

    private void sendIdleNotify(Window window, Pixmap pixmap, int serial, int idleFence) {
        if (idleFence != 0) syncExtension.setTriggered(idleFence);

        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                Event event = events.valueAt(i);
                if (event.window == window && event.mask.isSet(PresentIdleNotify.getEventMask())) {
                    event.client.sendEvent(new PresentIdleNotify(event.id, window, pixmap, serial, idleFence));
                }
            }
        }
    }

    private void sendCompleteNotify(Window window, int serial, long ust, long msc) {
        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                Event event = events.valueAt(i);
                if (event.window == window && event.mask.isSet(PresentCompleteNotify.getEventMask())) {
                    event.client.sendEvent(new PresentCompleteNotify(event.id, window, serial, Kind.PIXMAP, Mode.COPY, ust, msc));
                }
            }
        }
    }

    private static void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(1);
            outputStream.writeInt(0);
            outputStream.writePad(16);
        }
    }

    private void presentPixmap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        int pixmapId = inputStream.readInt();
        int serial = inputStream.readInt();
        inputStream.skip(8);
        short xOff = inputStream.readShort();
        short yOff = inputStream.readShort();
        inputStream.skip(8);
        int idleFence = inputStream.readInt();
        inputStream.skip(client.getRemainingRequestLength());

        final Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        final Pixmap pixmap = client.xServer.pixmapManager.getPixmap(pixmapId);
        if (pixmap == null) throw new BadPixmap(pixmapId);

        Drawable content = window.getContent();
        if (content.visual.depth != pixmap.drawable.visual.depth) throw new BadMatch();

        long ust = System.nanoTime() / 1000;
        long msc = ust / FAKE_INTERVAL;

        synchronized (content.renderLock) {
            content.copyArea((short)0, (short)0, xOff, yOff, pixmap.drawable.width, pixmap.drawable.height, pixmap.drawable);
            sendIdleNotify(window, pixmap, serial, idleFence);
            sendCompleteNotify(window, serial, ust, msc);
        }
    }

    private void selectInput(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int eventId = inputStream.readInt();
        int windowId = inputStream.readInt();
        Bitmask mask = new Bitmask(inputStream.readInt());

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        if (GPUImage.isSupported() && !mask.isEmpty()) {
            Drawable content = window.getContent();
            final Texture oldTexture = content.getTexture();
            XServerView xServerView = client.xServer.getRenderer().xServerView;
            Objects.requireNonNull(oldTexture);
            xServerView.queueEvent(() -> VortekRendererComponent.destroyTexture(oldTexture));
            content.setTexture(new GPUImage(content.width, content.height));
        }

        synchronized (events) {
            Event event = events.get(eventId);
            if (event != null) {
                if (event.window != window || event.client != client) throw new BadMatch();

                if (!mask.isEmpty()) {
                    event.mask = mask;
                }
                else events.remove(eventId);
            }
            else {
                event = new Event();
                event.id = eventId;
                event.window = window;
                event.client = client;
                event.mask = mask;
                events.put(eventId, event);
            }
        }
    }

    private void notifyMsc(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);
        int windowId = inputStream.readInt();
        inputStream.skip(4);

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        long ust = System.nanoTime() / 1000;
        long msc = ust / FAKE_INTERVAL;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0);
            outputStream.writeLong(ust);
            outputStream.writeLong(msc);
            outputStream.writePad(8);
        }
    }

    private void queryCapabilities(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int target = inputStream.readInt();

        int capabilities = CAPABILITY_ASYNC | CAPABILITY_UST;
        if (syncExtension != null) {
            capabilities |= CAPABILITY_FENCE;
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte) 0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(capabilities);
            outputStream.writePad(20);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        if (syncExtension == null) syncExtension = client.xServer.getExtension(SyncExtension.MAJOR_OPCODE);

        switch (opcode) {
            case ClientOpcodes.QUERY_VERSION :
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.PRESENT_PIXMAP:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.PIXMAP_MANAGER)) {
                    presentPixmap(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.SELECT_INPUT:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    selectInput(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.NOTIFY_MSC:
                notifyMsc(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_CAPABILITIES:
                queryCapabilities(client, inputStream, outputStream);
                break;
            default:
                Timber.w("[PresentExtension] Unimplemented opcode: %d", opcode);
                throw new BadImplementation();
        }
    }
}
