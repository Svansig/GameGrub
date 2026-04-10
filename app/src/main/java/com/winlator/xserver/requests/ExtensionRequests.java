package com.winlator.xserver.requests;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.XClient;
import com.winlator.xserver.extensions.Extension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class ExtensionRequests {
    public static void listExtensions(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        // Collect extension names
        int count = client.xServer.extensions.size();
        byte[][] names = new byte[count][];
        int totalBytes = 0;
        for (int i = 0; i < count; i++) {
            byte[] nameBytes = client.xServer.extensions.valueAt(i).getName().getBytes(StandardCharsets.US_ASCII);
            names[i] = nameBytes;
            totalBytes += 1 + nameBytes.length; // 1 byte length prefix + name bytes
        }
        int pad = (-totalBytes) & 3;
        int replyLength = (totalBytes + pad) / 4;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)count);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(replyLength);
            outputStream.writePad(24);
            for (byte[] name : names) {
                outputStream.writeByte((byte)name.length);
                outputStream.write(name);
            }
            if (pad > 0) outputStream.writePad(pad);
        }
    }

    public static void queryExtension(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException {
        short length = inputStream.readShort();
        inputStream.skip(2);
        String name = inputStream.readString8(length);
        Extension extension = client.xServer.getExtensionByName(name);
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);

            if (extension != null) {
                outputStream.writeByte((byte)1);
                outputStream.writeByte(extension.getMajorOpcode());
                outputStream.writeByte(extension.getFirstEventId());
                outputStream.writeByte(extension.getFirstErrorId());
                outputStream.writePad(20);
            }
            else {
                outputStream.writeByte((byte)0);
                outputStream.writePad(23);
            }
        }
    }
}
