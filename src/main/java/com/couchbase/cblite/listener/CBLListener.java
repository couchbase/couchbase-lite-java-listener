package com.couchbase.cblite.listener;


import com.couchbase.cblite.CBLManager;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;
import com.couchbase.cblite.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ScheduledExecutorService;

public class CBLListener implements Runnable {

    private Thread thread;
    private CBLManager manager;
    private CBLHTTPServer httpServer;
    public static final String TAG = "CBLListener";
    private int listenPort;
    private int serverStatus;

    //static inializer to ensure that cblite:// URLs are handled properly
    {
        CBLURLStreamHandlerFactory.registerSelfIgnoreError();
    }

    /**
     * CBLListener constructor
     *
     * @param server the CBLServerInternal instance
     * @param suggestedPort the suggested port to use.  if not available, will hunt for a new port.
     *                      and this port can be discovered by calling getListenPort()
     */
    public CBLListener(CBLManager manager, int suggestedPort) {
        this.manager = manager;
        this.httpServer = new CBLHTTPServer();
        this.httpServer.setManager(manager);
        this.httpServer.setListener(this);
        this.listenPort = discoverEmptyPort(suggestedPort);
        this.httpServer.setPort(this.listenPort);
    }

    /**
     * Hunt for an empty port starting from startPort by binding a server
     * socket until it succeeds w/o getting an exception.  Once found, close
     * the server socket so that port is available.
     *
     * Caveat: yes, there is a tiny race condition in the sense that the
     * caller could receive a port that was bound by another thread
     * before it has a chance to bind)
     *
     * @param startPort - the port to start hunting at, eg: 5984
     * @return the port that was bound.  (or a runtime exception is thrown)
     */
    public int discoverEmptyPort(int startPort) {
        for (int curPort=startPort; curPort<65536; curPort++) {
            try {
                ServerSocket socket = new ServerSocket(curPort);
                socket.close();
                return curPort;
            } catch (IOException e) {
                Log.d(CBLListener.TAG, "Could not bind to port: " + curPort + ".  Trying another port.");
            }

        }
        throw new RuntimeException("Could not find empty port starting from: " + startPort);
    }

    @Override
    public void run() {
        this.serverStatus = httpServer.serve();
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        httpServer.notifyStop();
    }

    public void onServerThread(Runnable r) {
        ScheduledExecutorService workExecutor = manager.getWorkExecutor();
        workExecutor.submit(r);
    }

    public int serverStatus() {
        return this.serverStatus;
    }

    public int getListenPort() {
        return listenPort;
    }
}
