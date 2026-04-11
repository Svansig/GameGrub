package com.winlator.alsaserver;

import com.winlator.xconnector.Client;
import com.winlator.xconnector.ConnectionHandler;

/**
 * Connection handler for incoming ALSA client connections.
 * Creates and manages ALSAClient instances for each connected client.
 */
public class ALSAClientConnectionHandler implements ConnectionHandler {
    private final ALSAClient.Options options;
    private final String containerVariant;

    public ALSAClientConnectionHandler(ALSAClient.Options options, String containerVariant) {
        this.options = options;
        this.containerVariant = containerVariant;
    }

    @Override
    public void handleNewConnection(Client client) {
        client.createIOStreams();
        client.setTag(new ALSAClient(this.options, this.containerVariant));
    }

    @Override
    public void handleConnectionShutdown(Client client) {
        ((ALSAClient)client.getTag()).release();
    }
}
