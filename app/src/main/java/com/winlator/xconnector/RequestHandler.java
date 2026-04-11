package com.winlator.xconnector;

import java.io.IOException;

/**
 * Interface for handlers that process X11 client requests. Implementations decode incoming
 * request data and generate appropriate responses.
 */
public interface RequestHandler {
    boolean handleRequest(Client client) throws IOException;
}
