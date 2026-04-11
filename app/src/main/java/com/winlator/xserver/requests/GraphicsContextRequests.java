package com.winlator.xserver.requests;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.GraphicsContext;
import com.winlator.xserver.Bitmask;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadDrawable;
import com.winlator.xserver.errors.BadGraphicsContext;
import com.winlator.xserver.errors.BadIdChoice;
import com.winlator.xserver.errors.BadValue;
import com.winlator.xserver.errors.XRequestError;

/**
 * X server request handlers for graphics context operations.
 * Handles CreateGC, CopyGC, ChangeGC, FreeGC, and SetClipRectangles requests.
 */
public abstract class GraphicsContextRequests {
    public static void createGC(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int gcId = inputStream.readInt();
        int drawableId = inputStream.readInt();
        Bitmask valueMask = new Bitmask(inputStream.readInt());

        if (!client.isValidResourceId(gcId)) throw new BadIdChoice(gcId);

        Drawable drawable = client.xServer.drawableManager.getDrawable(drawableId);
        if (drawable == null) throw new BadDrawable(drawableId);
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.createGraphicsContext(gcId, drawable);
        if (graphicsContext == null) throw new BadIdChoice(gcId);

        client.registerAsOwnerOfResource(graphicsContext);
        if (!valueMask.isEmpty()) client.xServer.graphicsContextManager.updateGraphicsContext(graphicsContext, valueMask, inputStream);
    }

    public static void copyGC(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int srcGCId = inputStream.readInt();
        int dstGCId = inputStream.readInt();
        Bitmask valueMask = new Bitmask(inputStream.readInt());
        GraphicsContext srcGC = client.xServer.graphicsContextManager.getGraphicsContext(srcGCId);
        GraphicsContext dstGC = client.xServer.graphicsContextManager.getGraphicsContext(dstGCId);
        if (srcGC == null) throw new BadGraphicsContext(srcGCId);
        if (dstGC == null) throw new BadGraphicsContext(dstGCId);
        if (!valueMask.isEmpty()) client.xServer.graphicsContextManager.copyGraphicsContext(srcGC, dstGC, valueMask);
    }

    public static void changeGC(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int gcId = inputStream.readInt();
        Bitmask valueMask = new Bitmask(inputStream.readInt());
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        if (!valueMask.isEmpty()) client.xServer.graphicsContextManager.updateGraphicsContext(graphicsContext, valueMask, inputStream);
    }

    public static void freeGC(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.xServer.graphicsContextManager.freeGraphicsContext(inputStream.readInt());
    }

    public static void setClipRectangles(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        int ordering = Byte.toUnsignedInt(client.getRequestData());
        if (ordering > 3) {
            throw new BadValue(ordering);
        }

        int gcId = inputStream.readInt();
        GraphicsContext graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId);
        if (graphicsContext == null) throw new BadGraphicsContext(gcId);

        short clipX = inputStream.readShort();
        short clipY = inputStream.readShort();
        graphicsContext.setClipOrigin(clipX, clipY);

        int length = client.getRemainingRequestLength();
        int rectCount = length / 8;
        if (rectCount == 0) {
            graphicsContext.setClipRects(null);
        } else {
            short[] rects = new short[rectCount * 4];
            int i = 0;
            while (length >= 8) {
                rects[i++] = inputStream.readShort(); // x
                rects[i++] = inputStream.readShort(); // y
                rects[i++] = inputStream.readShort(); // width
                rects[i++] = inputStream.readShort(); // height
                length -= 8;
            }
            graphicsContext.setClipRects(rects);
        }
    }
}
