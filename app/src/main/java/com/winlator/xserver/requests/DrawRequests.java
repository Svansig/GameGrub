package com.winlator.xserver.requests;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.GraphicsContext;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadDrawable;
import com.winlator.xserver.errors.BadGraphicsContext;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * DrawRequests - X11 drawing-related request handlers.
 * 
 * Handles X11 requests for drawing operations:
 * - PutImage: Upload image data to a drawable
 * - GetImage: Download image data from a drawable
 * - CopyArea: Copy a region from one drawable to another
 * - CopyPlane: Copy a single bit plane
 * - PolyPoint/Line/Segment/Rectangle: Basic shapes
 * - FillPoly: Filled polygons
 * - And more...
 * 
 * These operations draw on Drawable surfaces (Windows and Pixmaps).
 * GraphicsContext (GC) provides drawing parameters (foreground, background, etc.)
 * 
 * @see <a href="https://www.x.org/wiki/X11/">X11 Drawing Requests Specification</a>
 */
public abstract class DrawRequests {
    public enum Format {BITMAP, XY_PIXMAP, Z_PIXMAP}
    private enum CoordinateMode {ORIGIN, PREVIOUS}

    public static void putImage(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        Format format = Format.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        byte leftPad = inputStream.readByte();
        byte depth = inputStream.readByte();
        inputStream.skip(2);
        int length = client.getRemainingRequestLength();
        ByteBuffer data = inputStream.readByteBuffer(length);

        Drawable drawable =  client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);

        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        if (!(graphicsContext.getFunction() == GraphicsContext.Function.COPY || format == Format.Z_PIXMAP)) {
            throw new BadImplementation();
        }

        switch (format) {
            case BITMAP:
                if (leftPad != 0) throw new BadImplementation();
                if (depth == 1) {
                    drawable.drawImage((short)0, (short)0, dstX, dstY, width, height, (byte)1, data, width, height);
                }
                else throw new BadMatch();
                break;
            case XY_PIXMAP:
                if (drawable.visual.depth != depth) throw new BadMatch();
                break;
            case Z_PIXMAP:
                if (leftPad == 0) {
                    drawable.drawImage((short)0, (short)0, dstX, dstY, width, height, depth, data, width, height);
                }
                else throw new BadMatch();
                break;
        }
    }

    public static void getImage(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        Format format = Format.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        short x = inputStream.readShort();
        short y = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        inputStream.skip(4);

        if (format != Format.Z_PIXMAP) throw new BadImplementation();

        Drawable drawable =  client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        int visualId = client.xServer.pixmapManager.getPixmap(drawableId) == null ? drawable.visual.id : 0;
        ByteBuffer data = drawable.getImage(x, y, width, height);
        int length = data.limit();

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte(drawable.visual.depth);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt((length + 3) / 4);
            outputStream.writeInt(visualId);
            outputStream.writePad(20);
            outputStream.write(data);
            if ((-length & 3) > 0) outputStream.writePad(-length & 3);
        }
    }

    public static void copyArea(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int srcDrawableId = inputStream.readInt();
        int dstDrawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();

        Drawable srcDrawable =  client.xServer.drawableManager.getDrawable(srcDrawableId);
        if (srcDrawable == null) throw new BadDrawable(srcDrawableId);

        Drawable dstDrawable =  client.xServer.drawableManager.getDrawable(dstDrawableId);
        if (dstDrawable == null) throw new BadDrawable(dstDrawableId);

        GraphicsContext graphicsContext =  client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        if (srcDrawable.visual.depth != dstDrawable.visual.depth) throw new BadMatch();

        dstDrawable.copyArea(srcX, srcY, dstX, dstY, width, height, srcDrawable, graphicsContext.getFunction());
    }

    public static void polyLine(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        CoordinateMode coordinateMode = CoordinateMode.values()[client.getRequestData()];
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);
        int length = client.getRemainingRequestLength();

        short[] points = new short[length / 2];
        int i = 0;
        while (length != 0) {
            points[i++] = inputStream.readShort();
            points[i++] = inputStream.readShort();
            length -= 4;
        }

        if (coordinateMode == CoordinateMode.PREVIOUS && points.length >= 4) {
            for (int j = 2; j < points.length; j += 2) {
                points[j] += points[j - 2];
                points[j + 1] += points[j - 1];
            }
        }
        if (graphicsContext.getLineWidth() > 0) {
            drawable.drawLines(graphicsContext.getForeground(), graphicsContext.getLineWidth(), points);
        }
    }

    public static void polySegment(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        int length = client.getRemainingRequestLength();
        while (length >= 8) {
            short x1 = inputStream.readShort();
            short y1 = inputStream.readShort();
            short x2 = inputStream.readShort();
            short y2 = inputStream.readShort();
            drawable.drawLine(x1, y1, x2, y2, graphicsContext.getForeground(), graphicsContext.getLineWidth());
            length -= 8;
        }
    }

    public static void polyRectangle(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        int length = client.getRemainingRequestLength();
        while (length >= 8) {
            short x = inputStream.readShort();
            short y = inputStream.readShort();
            short width = inputStream.readShort();
            short height = inputStream.readShort();

            if (width > 0 && height > 0) {
                int lineWidth = graphicsContext.getLineWidth();
                int color = graphicsContext.getForeground();
                drawable.drawLine(x, y, x + width, y, color, lineWidth);
                drawable.drawLine(x, y, x, y + height, color, lineWidth);
                drawable.drawLine(x + width, y, x + width, y + height, color, lineWidth);
                drawable.drawLine(x, y + height, x + width, y + height, color, lineWidth);
            }
            length -= 8;
        }
    }

    public static void polyFillRectangle(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);
        int length = client.getRemainingRequestLength();

        while (length != 0) {
            short x = inputStream.readShort();
            short y = inputStream.readShort();
            short width = inputStream.readShort();
            short height = inputStream.readShort();
            drawable.fillRect(x, y, width, height, graphicsContext.getForeground());
            length -= 8;
        }
    }

    /** FillPoly (opcode 64): scanline even-odd fill of an arbitrary polygon. */
    public static void fillPoly(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        // requestData = shape (unused for rendering; we do the same regardless)
        int drawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        inputStream.skip(1); // shape
        byte coordinateModeRaw = inputStream.readByte();
        inputStream.skip(2); // pad
        CoordinateMode coordinateMode = coordinateModeRaw == 1 ? CoordinateMode.PREVIOUS : CoordinateMode.ORIGIN;

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        int length = client.getRemainingRequestLength();
        int nPts = length / 4;
        if (nPts < 3) {
            client.skipRequest();
            return;
        }

        int[] xs = new int[nPts];
        int[] ys = new int[nPts];
        for (int i = 0; i < nPts; i++) {
            xs[i] = inputStream.readShort();
            ys[i] = inputStream.readShort();
        }

        // Convert relative coords to absolute
        if (coordinateMode == CoordinateMode.PREVIOUS) {
            for (int i = 1; i < nPts; i++) {
                xs[i] += xs[i - 1];
                ys[i] += ys[i - 1];
            }
        }

        // Find Y bounds
        int minY = ys[0], maxY = ys[0];
        for (int i = 1; i < nPts; i++) {
            if (ys[i] < minY) minY = ys[i];
            if (ys[i] > maxY) maxY = ys[i];
        }
        minY = Math.max(minY, 0);
        maxY = Math.min(maxY, drawable.height - 1);

        int color = graphicsContext.getForeground();

        // Scanline fill: for each scanline find edge intersections, fill between pairs
        int[] intersections = new int[nPts];
        for (int scanY = minY; scanY <= maxY; scanY++) {
            int count = 0;
            for (int i = 0, j = nPts - 1; i < nPts; j = i++) {
                int yi = ys[i], yj = ys[j];
                if ((yi <= scanY && yj > scanY) || (yj <= scanY && yi > scanY)) {
                    // Compute X intersection using integer arithmetic
                    int xi = xs[i], xj = xs[j];
                    int x = xi + (scanY - yi) * (xj - xi) / (yj - yi);
                    intersections[count++] = x;
                }
            }
            Arrays.sort(intersections, 0, count);
            for (int k = 0; k + 1 < count; k += 2) {
                int x0 = intersections[k];
                int x1 = intersections[k + 1];
                if (x1 > x0) {
                    drawable.fillRectNoUpdate(x0, scanY, x1 - x0, 1, color);
                }
            }
        }
        drawable.forceUpdate();
    }

    /** GetMotionEvents (opcode 39): always returns an empty motion event list. */
    public static void getMotionEvents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest(); // start, stop timestamps
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0); // reply_length = 0
            outputStream.writeInt(0); // nEvents = 0
            outputStream.writePad(20);
        }
    }

    /** CopyPlane (opcode 61): extract 1-bit plane and blit using GC fg/bg colors.
     *  Simplified: treats the plane as a 1-bpp bitmap and draws it via drawImage. */
    public static void copyPlane(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int srcDrawableId = inputStream.readInt();
        int dstDrawableId = inputStream.readInt();
        int gcId = inputStream.readInt();
        short srcX = inputStream.readShort();
        short srcY = inputStream.readShort();
        short dstX = inputStream.readShort();
        short dstY = inputStream.readShort();
        short width = inputStream.readShort();
        short height = inputStream.readShort();
        inputStream.skip(4); // bitPlane

        Drawable srcDrawable = client.xServer.drawableManager.getDrawable(srcDrawableId);
        if (srcDrawable == null) throw new BadDrawable(srcDrawableId);
        Drawable dstDrawable = client.xServer.drawableManager.getDrawable(dstDrawableId);
        if (dstDrawable == null) throw new BadDrawable(dstDrawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        // Extract the region and copy it — use copyArea as a simplified approximation
        dstDrawable.copyArea(srcX, srcY, dstX, dstY, width, height, srcDrawable, graphicsContext.getFunction());
    }

    /** PolyArc / PolyFillArc (opcodes 63/68): skip — arc rendering not implemented. */
    public static void skipDrawRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }

    /** PolyText8, PolyText16, ImageText8, ImageText16 (69/71/74/75): text not rendered; consume and discard. */
    public static void skipTextRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }
}
