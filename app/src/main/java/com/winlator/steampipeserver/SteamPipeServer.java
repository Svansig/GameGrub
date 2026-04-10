package com.winlator.steampipeserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class SteamPipeServer {
    private static final int PORT = 34865;
    private static final long STOP_JOIN_TIMEOUT_MS = 2000L;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private Thread serverThread;
    private final Set<Socket> clientSockets = Collections.synchronizedSet(new HashSet<>());
    private final Set<Thread> clientThreads = Collections.synchronizedSet(new HashSet<>());

    private int readNetworkInt(DataInputStream input) throws IOException {
        return Integer.reverseBytes(input.readInt());
    }

    private void writeNetworkInt(DataOutputStream output, int value) throws IOException {
        output.writeInt(Integer.reverseBytes(value));
    }

    public synchronized void start() {
        stop();
        running = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                Timber.tag("SteamPipeServer").d("Server started on port " + PORT);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        if (!running) {
                            closeQuietly(clientSocket);
                            break;
                        }
                        clientSockets.add(clientSocket);
                        handleClient(clientSocket);
                    } catch (SocketException e) {
                        if (running) {
                            Timber.tag("SteamPipeServer").e(e, "Server socket failure while running");
                        } else {
                            Timber.tag("SteamPipeServer").d("Server socket closed during shutdown");
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                if (running) {
                    Timber.tag("SteamPipeServer").e(e, "Server error");
                } else {
                    Timber.tag("SteamPipeServer").d("Server stopped with socket close");
                }
            } finally {
                closeQuietly(serverSocket);
                serverSocket = null;
            }
        }, "SteamPipeServer-Accept");
        serverThread.start();
    }

    private void handleClient(Socket clientSocket) {
        Thread clientThread = new Thread(() -> {
            try (Socket socket = clientSocket;
                 DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                 DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

                while (running && !socket.isClosed()) {
                    if (input.available() > 0) {
                        int messageType = readNetworkInt(input);

                        switch (messageType) {
                            case RequestCodes.MSG_INIT:
                                Timber.tag("SteamPipeServer").d("Received MSG_INIT");
                                writeNetworkInt(output, 1);
                                output.flush();
                                break;
                            case RequestCodes.MSG_SHUTDOWN:
                                Timber.tag("SteamPipeServer").d("Received MSG_SHUTDOWN");
                                socket.close();
                                break;
                            case RequestCodes.MSG_RESTART_APP:
                                Timber.tag("SteamPipeServer").d("Received MSG_RESTART_APP");
                                int appId = input.readInt();
                                writeNetworkInt(output, 0); // Send restart not needed
                                break;
                            case RequestCodes.MSG_IS_RUNNING:
                                Timber.tag("SteamPipeServer").d("Received MSG_IS_RUNNING");
                                writeNetworkInt(output, 1); // Send Steam running status
                                break;
                            case RequestCodes.MSG_REGISTER_CALLBACK:
                                Timber.tag("SteamPipeServer").d("Received MSG_REGISTER_CALLBACK");
                                break;
                            case RequestCodes.MSG_UNREGISTER_CALLBACK:
                                Timber.tag("SteamPipeServer").d("Received MSG_UNREGISTER_CALLBACK");
                                break;
                            case RequestCodes.MSG_RUN_CALLBACKS:
                                Timber.tag("SteamPipeServer").d("Received MSG_RUN_CALLBACKS");
                                break;
                            default:
                                Timber.tag("SteamPipeServer").w("Unknown message type: " + messageType);
                                break;
                        }
                    }
                }
            } catch (SocketException e) {
                if (running) {
                    Timber.tag("SteamPipeServer").e(e, "Client socket failure while running");
                } else {
                    Timber.tag("SteamPipeServer").d("Client socket closed during shutdown");
                }
            } catch (IOException e) {
                if (running) {
                    Timber.tag("SteamPipeServer").e(e, "Client handler error");
                } else {
                    Timber.tag("SteamPipeServer").d("Client handler stopped during shutdown");
                }
            } finally {
                clientSockets.remove(clientSocket);
                clientThreads.remove(Thread.currentThread());
            }
        }, "SteamPipeServer-Client");
        clientThreads.add(clientThread);
        clientThread.start();
    }

    public synchronized void stop() {
        running = false;

        closeQuietly(serverSocket);

        ArrayList<Socket> socketsSnapshot;
        synchronized (clientSockets) {
            socketsSnapshot = new ArrayList<>(clientSockets);
        }
        for (Socket client : socketsSnapshot) {
            closeQuietly(client);
        }

        joinThread(serverThread, "accept");
        serverThread = null;

        ArrayList<Thread> threadsSnapshot;
        synchronized (clientThreads) {
            threadsSnapshot = new ArrayList<>(clientThreads);
        }
        for (Thread thread : threadsSnapshot) {
            joinThread(thread, "client");
        }
        clientThreads.clear();
        clientSockets.clear();
    }

    private void joinThread(Thread thread, String type) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(STOP_JOIN_TIMEOUT_MS);
            if (thread.isAlive()) {
                Timber.tag("SteamPipeServer").w("Timed out waiting for %s thread to stop", type);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Timber.tag("SteamPipeServer").w(e, "Interrupted while waiting for %s thread to stop", type);
        }
    }

    private void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(ServerSocket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
