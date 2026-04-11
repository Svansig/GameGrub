package com.winlator.xconnector;

import android.util.SparseArray;
import androidx.annotation.Keep;
import app.gamegrub.BuildConfig;
import java.io.IOException;
import java.nio.ByteBuffer;

import timber.log.Timber;

/**
 * XConnectorEpoll - X11 client connection handler using epoll.
 * 
 * Main X11 connection server:
 * - Uses epoll for efficient I/O multiplexing
 * - Manages Unix domain socket connections
 * - Handles multiple X11 clients
 * - Calls ConnectionHandler for new connections
 * - Calls RequestHandler for requests
 * 
 * The epoll-based design handles many concurrent
 * X11 clients efficiently.
 * 
 * @see <a href="https://man7.org/linux/man-pages/man7/epoll.7.html">epoll(7)</a>
 */
public class XConnectorEpoll implements Runnable {
    private static final String TAG = "XConnectorEpoll";
    private static final long STOP_JOIN_TIMEOUT_MS = 2000L;
    private static final long STOP_JOIN_POLL_MS = 100L;
    private static final long CLIENT_STOP_JOIN_TIMEOUT_MS = 1000L;

    private final String connectorLabel;
    private final ConnectionHandler connectionHandler;
    private final int epollFd;
    private Thread epollThread;
    private final RequestHandler requestHandler;
    private final int serverFd;
    private final int shutdownFd;
    private boolean running = false;
    private boolean multithreadedClients = false;
    private boolean canReceiveAncillaryMessages = false;
    private int initialInputBufferCapacity = 128;
    private int initialOutputBufferCapacity = 128;
    private final SparseArray<Client> connectedClients = new SparseArray<>();

    private native boolean addFdToEpoll(int i, int i2);

    public static native void closeFd(int i);

    private native int createAFUnixSocket(String str);

    private native int createEpollFd();

    private native int createEventFd();

    public static native String getNativePerfStats(boolean reset);

    private native boolean doEpollIndefinitely(int i, int i2, boolean z);

    private native void removeFdFromEpoll(int i, int i2);

    private native boolean waitForSocketRead(int i, int i2);

    static {
        System.loadLibrary("winlator");
    }

    public XConnectorEpoll(UnixSocketConfig socketConfig, ConnectionHandler connectionHandler, RequestHandler requestHandler) {
        this.connectionHandler = connectionHandler;
        this.requestHandler = requestHandler;
        this.connectorLabel = socketConfig.path + " [" + connectionHandler.getClass().getSimpleName() + "/" + requestHandler.getClass().getSimpleName() + "]";
        int createAFUnixSocket = createAFUnixSocket(socketConfig.path);
        this.serverFd = createAFUnixSocket;
        if (createAFUnixSocket < 0) {
            throw new RuntimeException("Failed to create an AF_UNIX socket.");
        }
        int createEpollFd = createEpollFd();
        this.epollFd = createEpollFd;
        if (createEpollFd < 0) {
            closeFd(createAFUnixSocket);
            throw new RuntimeException("Failed to create epoll fd.");
        }
        if (!addFdToEpoll(createEpollFd, createAFUnixSocket)) {
            closeFd(createAFUnixSocket);
            closeFd(createEpollFd);
            throw new RuntimeException("Failed to add server fd to epoll.");
        }
        int createEventFd = createEventFd();
        this.shutdownFd = createEventFd;
        if (!addFdToEpoll(createEpollFd, createEventFd)) {
            closeFd(createAFUnixSocket);
            closeFd(createEventFd);
            closeFd(createEpollFd);
            throw new RuntimeException("Failed to add shutdown fd to epoll.");
        }
        this.epollThread = new Thread(this, "XConnectorEpoll:" + this.connectorLabel);
    }

    private String logPrefix() {
        return "[" + this.connectorLabel + "]";
    }

    public synchronized void start() {
        Thread thread;
        if (!this.running && (thread = this.epollThread) != null) {
            this.running = true;
            Timber.tag(TAG).d(logPrefix() + " Starting connector thread (epollFd=" + this.epollFd + ", serverFd=" + this.serverFd + ", shutdownFd=" + this.shutdownFd + ")");
            thread.start();
        }
    }

    public synchronized void stop() {
        if (this.running && this.epollThread != null) {
            Timber.tag(TAG).d(logPrefix() + " Stopping connector thread (connectedClients=" + this.connectedClients.size() + ")");
            this.running = false;
            requestShutdown();

            long deadline = System.currentTimeMillis() + STOP_JOIN_TIMEOUT_MS;
            while (this.epollThread.isAlive() && System.currentTimeMillis() < deadline) {
                try {
                    this.epollThread.join(STOP_JOIN_POLL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Timber.tag(TAG).w(e, logPrefix() + " Interrupted while waiting for connector thread to stop");
                    break;
                }
            }

            if (this.epollThread.isAlive()) {
                Timber.tag(TAG).w(logPrefix() + " Connector thread did not stop within timeout");
                this.epollThread.interrupt();
            }

            this.epollThread = null;

            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).d(logPrefix() + " Native JNI perf: " + getNativePerfStats(true));
            }
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        while (this.running) {
            boolean monitorClients = true;
            if (!doEpollIndefinitely(this.epollFd, this.serverFd, !this.multithreadedClients && monitorClients)) {
                if (this.running) {
                    Timber.tag(TAG).e(logPrefix() + " epoll loop exited unexpectedly; shutting down all X clients (epollFd=" + this.epollFd + ", serverFd=" + this.serverFd + ", shutdownFd=" + this.shutdownFd + ", connectedClients=" + this.connectedClients.size() + ", multithreadedClients=" + this.multithreadedClients + ", monitorClients=" + monitorClients + ")");
                }
                break;
            }
        }
        shutdown();
    }

    @Keep
    private void handleNewConnection(int fd) {
        final Client client = new Client(this, new ClientSocket(fd));
        client.connected = true;
        if (this.multithreadedClients) {
            client.shutdownFd = createEventFd();
            client.pollThread = new Thread(() -> {
                connectionHandler.handleNewConnection(client);
                while (client.connected &&                        // stay in loop
                        waitForSocketRead(client.clientSocket.fd,  // until socket readable
                                client.shutdownFd)) { }  //   or shutdown signalled
            });
            client.pollThread.start();
        } else {
            this.connectionHandler.handleNewConnection(client);
        }
        this.connectedClients.put(fd, client);
    }

    @Keep
    private void handleExistingConnection(int fd) {
        Client client = this.connectedClients.get(fd);
        if (client == null) {
            return;
        }
        XInputStream inputStream = client.getInputStream();
        try {
            if (inputStream != null) {
                if (inputStream.readMoreData(this.canReceiveAncillaryMessages) > 0) {
                    int activePosition = 0;
                    while (this.running && this.requestHandler.handleRequest(client)) {
                        activePosition = inputStream.getActivePosition();
                    }
                    inputStream.setActivePosition(activePosition);
                    return;
                }
                killConnection(client);
                return;
            }
            this.requestHandler.handleRequest(client);
        } catch (IOException e) {
            killConnection(client);
        }
    }

    public Client getClient(int fd) {
        return this.connectedClients.get(fd);
    }

    public void killConnection(Client client) {
        client.connected = false;
        if (this.multithreadedClients) {
            if (client.pollThread != null && Thread.currentThread() != client.pollThread) {
                client.requestShutdown();
                long deadline = System.currentTimeMillis() + CLIENT_STOP_JOIN_TIMEOUT_MS;
                while (client.pollThread.isAlive() && System.currentTimeMillis() < deadline) {
                    try {
                        client.pollThread.join(STOP_JOIN_POLL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Timber.tag(TAG).w(e, logPrefix() + " Interrupted while waiting for client poll thread to stop");
                        break;
                    }
                }
                if (client.pollThread.isAlive()) {
                    Timber.tag(TAG).w(logPrefix() + " Client poll thread did not stop within timeout");
                    client.pollThread.interrupt();
                }
                this.connectionHandler.handleConnectionShutdown(client);
                client.pollThread = null;
            }
            closeFd(client.shutdownFd);
        } else {
            this.connectionHandler.handleConnectionShutdown(client);
            removeFdFromEpoll(this.epollFd, client.clientSocket.fd);
        }
        closeFd(client.clientSocket.fd);
        this.connectedClients.remove(client.clientSocket.fd);
    }

    private void shutdown() {
        while (this.connectedClients.size() > 0) {
            Client client = this.connectedClients.valueAt(this.connectedClients.size() - 1);
            killConnection(client);
        }
        removeFdFromEpoll(this.epollFd, this.serverFd);
        removeFdFromEpoll(this.epollFd, this.shutdownFd);
        closeFd(this.serverFd);
        closeFd(this.shutdownFd);
        closeFd(this.epollFd);
    }

    public int getInitialInputBufferCapacity() {
        return this.initialInputBufferCapacity;
    }

    public void setInitialInputBufferCapacity(int initialInputBufferCapacity) {
        this.initialInputBufferCapacity = initialInputBufferCapacity;
    }

    public int getInitialOutputBufferCapacity() {
        return this.initialOutputBufferCapacity;
    }

    public void setInitialOutputBufferCapacity(int initialOutputBufferCapacity) {
        this.initialOutputBufferCapacity = initialOutputBufferCapacity;
    }

    public void setMultithreadedClients(boolean multithreadedClients) {
        this.multithreadedClients = multithreadedClients;
    }

    public void setCanReceiveAncillaryMessages(boolean canReceiveAncillaryMessages) {
        this.canReceiveAncillaryMessages = canReceiveAncillaryMessages;
    }

    public int getConnectedClientsCount() {
        return this.connectedClients.size();
    }

    public Client getConnectedClientAt(int index) {
        if (index >= 0 && index < this.connectedClients.size()) {
            return this.connectedClients.valueAt(index);
        }
        return null;
    }

    private void requestShutdown() {
        try {
            ByteBuffer data = ByteBuffer.allocateDirect(8);
            data.asLongBuffer().put(1L);
            new ClientSocket(this.shutdownFd).write(data);
        } catch (IOException ignored) {
        }
    }
}
