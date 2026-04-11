package com.winlator.sysvshm;

import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/**
 * Connection handler for System V Shared Memory (SysV SHM) sockets. Initializes new client
 * sessions by creating I/O streams and attaching the SysVSharedMemory instance as client tag.
 */
public class SysVSHMConnectionHandler implements ConnectionHandler {
    private final SysVSharedMemory sysVSharedMemory;

    public SysVSHMConnectionHandler(SysVSharedMemory sysVSharedMemory) {
        this.sysVSharedMemory = sysVSharedMemory;
    }

    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(sysVSharedMemory);
    }

    @Override
    public void handleConnectionShutdown(Client client) {}
}
