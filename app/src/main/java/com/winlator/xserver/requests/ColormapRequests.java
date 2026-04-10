package com.winlator.xserver.requests;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

/**
 * Handles X11 colormap opcodes (80–92).
 *
 * This server uses a TrueColor visual; clients compute pixel values directly
 * from color components. Colormap operations are therefore mostly synthetic
 * stubs that return plausible replies without maintaining real colormap state.
 */
public abstract class ColormapRequests {

    /**
     * AllocColor (84): allocate a color cell in the colormap.
     *
     * For TrueColor, we just compute the pixel from the 16-bit RGB components
     * (shift each channel down to 8 bits, pack as 0xRRGGBB) and return it.
     * The exact value is rounded to the server's color precision (8 bits/channel).
     */
    public static void allocColor(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4); // colormapId (ignored — we have one implicit colormap)
        int red   = inputStream.readShort() & 0xFFFF;
        int green = inputStream.readShort() & 0xFFFF;
        int blue  = inputStream.readShort() & 0xFFFF;
        inputStream.skip(2); // pad

        // Round 16-bit components to 8 bits
        int r8 = red   >> 8;
        int g8 = green >> 8;
        int b8 = blue  >> 8;
        int pixel = (r8 << 16) | (g8 << 8) | b8;

        // Round-trip: the "exact" values we can represent at 8 bits/channel
        int exactRed   = (r8 << 8) | r8;
        int exactGreen = (g8 << 8) | g8;
        int exactBlue  = (b8 << 8) | b8;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0); // reply_length
            outputStream.writeShort((short)exactRed);
            outputStream.writeShort((short)exactGreen);
            outputStream.writeShort((short)exactBlue);
            outputStream.writeShort((short)0); // pad
            outputStream.writeInt(pixel);
            outputStream.writePad(12);
        }
    }

    /**
     * AllocNamedColor (85): allocate a named color.
     * Simplified: return black (pixel=0) for any unknown name.
     */
    public static void allocNamedColor(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // colormapId
        short nameLen = inputStream.readShort();
        inputStream.skip(2); // pad
        inputStream.readString8(nameLen);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(0); // pixel = black
            outputStream.writeShort((short)0); // exact-red
            outputStream.writeShort((short)0); // exact-green
            outputStream.writeShort((short)0); // exact-blue
            outputStream.writeShort((short)0); // visual-red
            outputStream.writeShort((short)0); // visual-green
            outputStream.writeShort((short)0); // visual-blue
            outputStream.writePad(8);
        }
    }

    /**
     * AllocColorCells (86): allocate read/write color cells.
     * Simplified: return an empty allocation (nPixels=0, nMasks=0).
     */
    public static void allocColorCells(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest();
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0); // reply_length = 0 (no pixels, no masks)
            outputStream.writeShort((short)0); // nPixels
            outputStream.writeShort((short)0); // nMasks
            outputStream.writePad(20);
        }
    }

    /**
     * AllocColorPlanes (87): allocate color planes.
     * Simplified: return empty allocation.
     */
    public static void allocColorPlanes(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest();
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)0); // nPixels
            outputStream.writeShort((short)0); // pad
            outputStream.writeInt(0); // red-mask
            outputStream.writeInt(0); // green-mask
            outputStream.writeInt(0); // blue-mask
            outputStream.writePad(8);
        }
    }

    /** FreeColors (88): no-op — we don't track allocated cells. */
    public static void freeColors(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }

    /** StoreColors (89): no-op for TrueColor. */
    public static void storeColors(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }

    /** StoreNamedColor (90): no-op for TrueColor. */
    public static void storeNamedColor(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }

    /**
     * QueryColors (91): return RGB values for an array of pixel values.
     * For TrueColor (0xRRGGBB pixel layout), decompose each pixel back to
     * 16-bit RGB channels.
     */
    public static void queryColors(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // colormapId
        int length = client.getRemainingRequestLength();
        int nPixels = length / 4;
        int[] pixels = new int[nPixels];
        for (int i = 0; i < nPixels; i++) pixels[i] = inputStream.readInt();

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(nPixels * 2); // reply_length (each color = 8 bytes = 2 units)
            outputStream.writeShort((short)nPixels);
            outputStream.writePad(22);
            for (int pixel : pixels) {
                int r8 = (pixel >> 16) & 0xFF;
                int g8 = (pixel >> 8) & 0xFF;
                int b8 = pixel & 0xFF;
                outputStream.writeShort((short)((r8 << 8) | r8)); // expand to 16-bit
                outputStream.writeShort((short)((g8 << 8) | g8));
                outputStream.writeShort((short)((b8 << 8) | b8));
                outputStream.writeShort((short)0); // pad
            }
        }
    }

    /**
     * LookupColor (92): look up a named color.
     * Simplified: return black for any unknown name.
     */
    public static void lookupColor(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // colormapId
        short nameLen = inputStream.readShort();
        inputStream.skip(2);
        inputStream.readString8(nameLen);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            // exact and visual RGB, all zero (black)
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writePad(12);
        }
    }

    /**
     * ListInstalledColormaps (83): return the one default colormap.
     * colormapId=1 is the synthetic ID we use throughout.
     */
    public static void listInstalledColormaps(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(4); // windowId (ignored)
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(1); // reply_length = 1 (one 4-byte colormap ID)
            outputStream.writeShort((short)1); // nColormaps = 1
            outputStream.writePad(22);
            outputStream.writeInt(1); // the default colormap ID
        }
    }
}
