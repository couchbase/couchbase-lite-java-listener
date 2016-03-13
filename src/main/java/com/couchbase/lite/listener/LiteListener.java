package com.couchbase.lite.listener;

import com.couchbase.lite.Manager;
import com.couchbase.lite.router.URLStreamHandlerFactory;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class LiteListener implements Runnable {

    private static int DEF_SOCKET_TIMEOUT = 30 * 1000;

    private Thread thread;
    private Manager manager;
    private LiteServer httpServer;
    public static final String TAG = "LiteListener";
    private int listenPort;
    private int serverStatus;
    private int socketTimeout = DEF_SOCKET_TIMEOUT;

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    //static inializer to ensure that cblite:// URLs are handled properly
    {
        URLStreamHandlerFactory.registerSelfIgnoreError();
    }

    /**
     * LiteListener constructor
     *
     * @param manager the Manager instance
     * @param suggestedPort the suggested port to use.  if not available, will hunt for a new port.
     *                      and this port can be discovered by calling getListenPort()
     * @param allowedCredentials any clients connecting to this liteserv must present these
     *                           credentials.
     */

    public LiteListener(Manager manager, int suggestedPort, Credentials allowedCredentials, Properties tjwsProperties) {
        this.manager = manager;
        this.httpServer = new LiteServer();
        this.httpServer.setProps(tjwsProperties);
        this.httpServer.setManager(manager);
        this.httpServer.setListener(this);
        this.listenPort = discoverEmptyPort(suggestedPort);
        this.httpServer.setPort(this.listenPort);
        this.httpServer.setAllowedCredentials(allowedCredentials);
    }

    public LiteListener(Manager manager, int suggestedPort, Credentials allowedCredentials) {
        this(manager, suggestedPort, allowedCredentials, new Properties());
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
                Log.w(LiteListener.TAG, "Could not bind to port: %d.  Trying another port.", curPort);
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
