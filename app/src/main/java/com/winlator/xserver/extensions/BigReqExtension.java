package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.errors.BadRequest;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

/**
 * BigReqExtension - X11 BIG-REQUESTS extension implementation.
 * 
 * This extension increases the maximum request length from 256KB (65535 * 4 bytes)
 * to 16GB (4194303 * 4 bytes) by allowing an extra 32-bit length field.
 * 
 * This is critical for modern applications that send large images or data in a single request.
 * 
 * @see <a href="https://www.x.org/wiki/Extensions/">X11 BigReq Extension</a>
 */
public class BigReqExtension implements Extension {
    /** Major opcode for this extension (-100 to avoid conflict with core opcodes) */
    public static final byte MAJOR_OPCODE = -100;
    
    /** Maximum request length in 4-byte units: 16GB */
    private static final int MAX_REQUEST_LENGTH = 4194303;

    @Override
    public String getName() {
        return "BIG-REQUESTS";
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

    /**
     * Handles BigReq queries.
     * Only one request: QueryVersion (opcode 0) which returns the max request length.
     */
    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        if (client.getRequestData() != 0) {
            throw new BadRequest(Byte.toUnsignedInt(client.getRequestData()));
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(MAX_REQUEST_LENGTH);
            outputStream.writePad(20);
        }
    }
}
