package com.couchbase.cblite.listener;

import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Provides read only status on a socket.
 */
public class CBLSocketStatus {
    protected ServerSocket serverSocket;

    public CBLSocketStatus(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /**
     * Returns the port the acceptor has bound to.
     * @return
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the local IP address of the server socket or null if not bound.
     * @return
     */
    public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    /**
     * Returns if the server is bound to a local address and port
     * @return
     */
    public boolean isBound() {
        return serverSocket.isBound();
    }

    /**
     * Returns if the socket the server is listening on is closed
     * @return
     */
    public boolean isClosed() {
        return serverSocket.isClosed();
    }
}
