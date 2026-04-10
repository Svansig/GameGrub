package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.core.Callback;
import com.winlator.renderer.GPUImage;
import com.winlator.sysvshm.SysVSharedMemory;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Pixmap;
import com.winlator.xserver.Window;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadAlloc;
import com.winlator.xserver.errors.BadDrawable;
import com.winlator.xserver.errors.BadFence;
import com.winlator.xserver.errors.BadIdChoice;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadPixmap;
import com.winlator.xserver.errors.BadWindow;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * X11 DRI3 (Direct Rendering Infrastructure 3) Extension implementation.
 * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/dri3proto.txt">DRI3 Protocol Specification</a>
 */
public class DRI3Extension implements Extension {
    public static final byte MAJOR_OPCODE = -102;
    private final Callback<Drawable> onDestroyDrawableListener = (drawable) -> {
        ByteBuffer data = drawable.getData();
        SysVSharedMemory.unmapSHMSegment(data, data.capacity());
    };

    /**
     * DRI3 extension request opcodes.
     * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/dri3proto.txt">DRI3 Protocol - Extension Requests</a>
     */
    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte OPEN = 1;
        private static final byte PIXMAP_FROM_BUFFER = 2;
        /**
         * DRI3BufferFromPixmap (opcode 3) - Exports a server-side pixmap to a client-side buffer.
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/dri3proto.txt">DRI3BufferFromPixmap</a>
         */
        private static final byte BUFFER_FROM_PIXMAP = 3;
        private static final byte FENCE_FROM_FD = 4;
        private static final byte FD_FROM_FENCE = 5;
        private static final byte GET_SUPPORTED_FORMATS = 6;
        /**
         * DRI3PixmapFromBuffers (opcode 7) - Creates a pixmap from multiple buffers (v1.1).
         * @see <a href="https://gitlab.freedesktop.org/xorg/proto/xorgproto/-/blob/master/dri3proto.txt">DRI3PixmapFromBuffers</a>
         */
        private static final byte PIXMAP_FROM_BUFFERS = 7;
    }

    @Override
    public String getName() {
        return "DRI3";
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

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
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

    private void open(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int drawableId = inputStream.readInt();
        inputStream.skip(4); // provider

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);

        int fd = SysVSharedMemory.createMemoryFd("dri3-open", 0);
        if (fd < 0) throw new BadAlloc();

        try {
            try (XStreamLock lock = outputStream.lock()) {
                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte((byte)1); // nfd = 1
                outputStream.writeShort(client.getSequenceNumber());
                outputStream.writeInt(0); // reply length (no trailing strings)
                outputStream.writeShort((short)0); // driver-name-length
                outputStream.writeShort((short)0); // device-name-length
                outputStream.writePad(20);
            }
            outputStream.setAncillaryFd(fd);
        } catch (IOException e) {
            XConnectorEpoll.closeFd(fd);
            throw e;
        }
    }

    private void fenceFromFd(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int drawableId = inputStream.readInt();
        int fenceId = inputStream.readInt();
        boolean initiallyTriggered = inputStream.readByte() == 1;
        inputStream.skip(3);
        int fd = inputStream.getAncillaryFd();
        XConnectorEpoll.closeFd(fd);

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);

        SyncExtension sync = client.xServer.getExtension(SyncExtension.MAJOR_OPCODE);
        if (sync == null) throw new BadImplementation();
        sync.createFence(fenceId, initiallyTriggered);
    }

    private void fdFromFence(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int drawableId = inputStream.readInt();
        int fenceId = inputStream.readInt();

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);

        SyncExtension sync = client.xServer.getExtension(SyncExtension.MAJOR_OPCODE);
        if (sync == null || !sync.hasFence(fenceId)) throw new BadFence(fenceId);

        int fd = SysVSharedMemory.createMemoryFd("dri3-fence", 0);
        if (fd < 0) throw new BadAlloc();

        try {
            try (XStreamLock lock = outputStream.lock()) {
                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte((byte)1); // nfd = 1
                outputStream.writeShort(client.getSequenceNumber());
                outputStream.writeInt(0);
                outputStream.writePad(24);
            }
            outputStream.setAncillaryFd(fd);
        } catch (IOException e) {
            XConnectorEpoll.closeFd(fd);
            throw e;
        }
    }

    private void getSupportedModifiers(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        inputStream.skip(4); // depth + bpp + pad

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        // Advertise DRM_FORMAT_MOD_LINEAR (0) for both window and screen
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0); // nfd = 0
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(4); // reply length: 2 CARD64s = 4 CARD32 units
            outputStream.writeInt(1); // num_window_modifiers
            outputStream.writeInt(1); // num_screen_modifiers
            outputStream.writePad(16);
            outputStream.writeLong(0L); // DRM_FORMAT_MOD_LINEAR window
            outputStream.writeLong(0L); // DRM_FORMAT_MOD_LINEAR screen
        }
    }

    private void pixmapFromBuffer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int pixmapId = inputStream.readInt();
        int windowId = inputStream.readInt();
        int size = inputStream.readInt();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        short stride = inputStream.readShort();
        byte depth = inputStream.readByte();
        inputStream.skip(1);

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        Pixmap pixmap = client.xServer.pixmapManager.getPixmap(pixmapId);
        if (pixmap != null) throw new BadIdChoice(pixmapId);

        int fd = inputStream.getAncillaryFd();
        pixmapFromFd(client, pixmapId, width, height, stride, 0, depth, fd, size);
    }

    private void pixmapFromBuffers(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int pixmapId = inputStream.readInt();
        int windowId = inputStream.readInt();
        inputStream.skip(4);
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        int stride = inputStream.readInt();
        int offset = inputStream.readInt();
        inputStream.skip(24);
        byte depth = inputStream.readByte();
        inputStream.skip(3);
        long modifiers = inputStream.readLong();

        Window window = client.xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);
        Pixmap pixmap = client.xServer.pixmapManager.getPixmap(pixmapId);
        if (pixmap != null) throw new BadIdChoice(pixmapId);

        int fd = inputStream.getAncillaryFd();
        long size = (long)stride * height;

        if (modifiers == 1255) {
            pixmapFromHardwareBuffer(client, pixmapId, width, height, depth, fd);
        }
        else if (modifiers == 1274) {
            pixmapFromFd(client, pixmapId, width, height, stride, offset, depth, fd, size);
        }
        else {
            XConnectorEpoll.closeFd(fd);
            throw new BadImplementation();
        }
    }

    private void pixmapFromHardwareBuffer(XClient client, int pixmapId, short width, short height, byte depth, int fd) throws IOException {
        try {
            GPUImage gpuImage = new GPUImage(fd);
            Drawable drawable = client.xServer.drawableManager.createDrawable(pixmapId, gpuImage.getStride(), height, depth);
            drawable.setTexture(gpuImage);
            client.xServer.pixmapManager.createPixmap(drawable);
        }
        finally {
            XConnectorEpoll.closeFd(fd);
        }
    }

    private void pixmapFromFd(XClient client, int pixmapId, short width, short height, int stride, int offset, byte depth, int fd, long size)  throws IOException, XRequestError {
        try {
            ByteBuffer buffer = SysVSharedMemory.mapSHMSegment(fd, size, offset, true);
            if (buffer == null) throw new BadAlloc();

            short totalWidth = (short)(stride / 4);
            Drawable drawable = client.xServer.drawableManager.createDrawable(pixmapId, totalWidth, height, depth);
            drawable.setData(buffer);
            drawable.setTexture(null);
            drawable.setOnDestroyListener(onDestroyDrawableListener);
            client.xServer.pixmapManager.createPixmap(drawable);
        }
        finally {
            XConnectorEpoll.closeFd(fd);
        }
    }

    private void bufferFromPixmap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int pixmapId = inputStream.readInt();

        Pixmap pixmap = client.xServer.pixmapManager.getPixmap(pixmapId);
        if (pixmap == null) throw new BadPixmap(pixmapId);

        Drawable drawable = pixmap.drawable;
        int size = drawable.width * drawable.height * (drawable.visual.depth / 8);

        int fd = SysVSharedMemory.createMemoryFd("dri3-buffer-from-pixmap", size);
        if (fd < 0) throw new BadAlloc();

        try {
            ByteBuffer buffer = SysVSharedMemory.mapSHMSegment(fd, size, 0, false);
            if (buffer == null) {
                XConnectorEpoll.closeFd(fd);
                throw new BadAlloc();
            }

            ByteBuffer src = drawable.getData();
            if (src != null) {
                src.position(0);
                src.limit(size);
                buffer.put(src);
            }
            SysVSharedMemory.unmapSHMSegment(buffer, size);

            try (XStreamLock lock = outputStream.lock()) {
                outputStream.writeByte(RESPONSE_CODE_SUCCESS);
                outputStream.writeByte((byte) 1);
                outputStream.writeShort(client.getSequenceNumber());
                outputStream.writeInt(0);
                outputStream.writeInt(drawable.width);
                outputStream.writeShort(drawable.height);
                outputStream.writeShort((short)drawable.width);
                outputStream.writeByte(drawable.visual.depth);
                outputStream.writeByte((byte)(drawable.visual.depth > 16 ? 32 : drawable.visual.depth));
                outputStream.writePad(12);
            }
            outputStream.setAncillaryFd(fd);
        }
        catch (IOException | XRequestError e) {
            XConnectorEpoll.closeFd(fd);
            throw e;
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        switch (opcode) {
            case ClientOpcodes.QUERY_VERSION :
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.OPEN :
                try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
                    open(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.PIXMAP_FROM_BUFFER:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                    pixmapFromBuffer(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.PIXMAP_FROM_BUFFERS:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                    pixmapFromBuffers(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.BUFFER_FROM_PIXMAP:
                try (XLock lock = client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER)) {
                    bufferFromPixmap(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.FENCE_FROM_FD:
                try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
                    fenceFromFd(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.FD_FROM_FENCE:
                try (XLock lock = client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER)) {
                    fdFromFence(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.GET_SUPPORTED_FORMATS:
                try (XLock lock = client.xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    getSupportedModifiers(client, inputStream, outputStream);
                }
                break;
            default:
                Timber.w("[DRI3] Unimplemented opcode: %d", opcode);
                throw new BadImplementation();
        }
    }
}
