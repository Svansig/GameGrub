package com.winlator.xserver;

import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/**
 * XClientConnectionHandler - Handles X11 client connections.
 * 
 * This is a ConnectionHandler for the XConnector that manages the lifecycle
 * of X11 client connections to the XServer.
 * 
 * Two callbacks:
 * - handleNewConnection: Creates XClient when application connects
 * - handleConnectionShutdown: Cleans up when application disconnects
 */
public class XClientConnectionHandler implements ConnectionHandler {
    /** The XServer that accepts connections */
    private final XServer xServer;

    /**
     * Creates a connection handler for an XServer.
     * 
     * @param xServer The XServer to handle connections for
     */
    public XClientConnectionHandler(XServer xServer) {
        this.xServer = xServer;
    }

    /**
     * Called when a new client connects.
     * Creates IO streams and XClient object for the connection.
     * 
     * @param client The client connection
     */
    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new XClient(xServer, client.getInputStream(), client.getOutputStream()));
    }

    /**
     * Called when a client disconnects.
     * Frees all resources owned by the client.
     * 
     * @param client The disconnected client
     */
    @Override
    public void handleConnectionShutdown(Client client) {
        ((XClient)client.getTag()).freeResources();
    }
}
