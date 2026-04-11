package com.winlator.xconnector;

/**
 * Interface for handling X11 client connection lifecycle events, including new connections
 * and connection shutdown notifications.
 */
public interface ConnectionHandler {
    void handleConnectionShutdown(Client client);

    void handleNewConnection(Client client);
}