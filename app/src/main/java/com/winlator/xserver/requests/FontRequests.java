package com.winlator.xserver.requests;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadName;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class FontRequests {
    public static void openFont(XClient client, XInputStream inputStream, XOutputStream outputStream) throws XRequestError {
        inputStream.skip(4);
        int length = inputStream.readShort();
        inputStream.skip(2);
        String name = inputStream.readString8(length);
        if (!name.equals("cursor")) throw new BadName();
    }

    public static void queryTextExtents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest(); // consume remaining bytes (string data)
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0); // draw-direction: LeftToRight
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)0); // font-ascent
            outputStream.writeShort((short)0); // font-descent
            outputStream.writeShort((short)0); // overall-ascent
            outputStream.writeShort((short)0); // overall-descent
            outputStream.writeInt(0); // overall-width
            outputStream.writeInt(0); // overall-left
            outputStream.writeInt(0); // overall-right
            outputStream.writePad(4);
        }
    }

    public static void listFonts(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        inputStream.skip(2);
        short patternLength = inputStream.readShort();
        inputStream.readString8(patternLength);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort((short)0);
            outputStream.writePad(22);
        }
    }

    /**
     * QueryFont (opcode 47): returns synthetic metrics for a fixed 8×16 cell font.
     * Reply length = 7 (fixed header only; 0 properties, 0 per-char infos).
     * xCharInfo = {left(2), right(2), width(2), ascent(2), descent(2), attrs(2)} = 12 bytes.
     */
    public static void queryFont(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest(); // consume fontId
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(7); // reply_length = 7 (7*4=28 bytes beyond 32-byte header = 60 total)

            // min-bounds xCharInfo (12 bytes): left=0, right=8, width=8, ascent=13, descent=3, attrs=0
            outputStream.writeShort((short)0);   // leftSideBearing
            outputStream.writeShort((short)8);   // rightSideBearing
            outputStream.writeShort((short)8);   // characterWidth
            outputStream.writeShort((short)13);  // ascent
            outputStream.writeShort((short)3);   // descent
            outputStream.writeShort((short)0);   // attributes
            outputStream.writePad(4);            // pad after minBounds

            // max-bounds xCharInfo (same metrics for fixed font)
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)8);
            outputStream.writeShort((short)8);
            outputStream.writeShort((short)13);
            outputStream.writeShort((short)3);
            outputStream.writeShort((short)0);
            outputStream.writePad(4);            // pad after maxBounds

            outputStream.writeShort((short)32);  // min-char-or-byte2 (space)
            outputStream.writeShort((short)126); // max-char-or-byte2 (tilde)
            outputStream.writeShort((short)32);  // default-char (space)
            outputStream.writeShort((short)0);   // nFontProperties = 0
            outputStream.writeByte((byte)0);     // draw-direction: LeftToRight
            outputStream.writeByte((byte)0);     // min-byte1
            outputStream.writeByte((byte)0);     // max-byte1
            outputStream.writeByte((byte)1);     // all-chars-exist = true
            outputStream.writeShort((short)13);  // font-ascent
            outputStream.writeShort((short)3);   // font-descent
            outputStream.writeInt(0);            // nCharInfos = 0 (all chars use max-bounds)
            // No font properties, no per-char infos follow
        }
    }

    /** ListFontsWithInfo (opcode 50): terminate immediately with the final-reply marker (name-length=0). */
    public static void listFontsWithInfo(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        client.skipRequest();
        try (XStreamLock lock = outputStream.lock()) {
            // Final reply: name-length=0 signals end of sequence
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0); // name-length = 0 (terminator)
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(7); // same layout as QueryFont fixed header
            // Synthetic zero metrics
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writePad(4);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writePad(4);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeByte((byte)0); outputStream.writeByte((byte)0); outputStream.writeByte((byte)0);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)0); outputStream.writeShort((short)0);
            outputStream.writeInt(0); // nReplies = 0
        }
    }

    /** SetFontPath (opcode 51): no-op; we don't manage font search paths. */
    public static void setFontPath(XClient client, XInputStream inputStream, XOutputStream outputStream) {
        client.skipRequest();
    }

    /** GetFontPath (opcode 52): return an empty font path list. */
    public static void getFontPath(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0); // reply_length
            outputStream.writeShort((short)0); // nStrings = 0
            outputStream.writePad(22);
        }
    }
}
