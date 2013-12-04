package com.couchbase.cblite.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ScheduledExecutorService;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import Acme.Serve.Serve;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.router.CBLRequestAuthorization;
import com.couchbase.cblite.router.CBLURLStreamHandlerFactory;
import com.couchbase.cblite.util.Log;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class CBLListener implements Runnable {

    private Thread thread;
    private final CBLServer server;
    private final CBLHTTPServer httpServer;
    public static final String TAG = "CBLListener";

    //static initializer to ensure that cblite:// URLs are handled properly
    {
        CBLURLStreamHandlerFactory.registerSelfIgnoreError();
    }

    /**
     * CBLListener constructor
     *
     * @param server the CBLServer instance
     * @param port the suggested port to use. If 0 is specified then the next available port will be picked.
     */
    public CBLListener(CBLServer server, int port) {
        this(server, port, new Properties(), null);
    }

    /**
     *
     * @param server the CBLServer instance
     * @param suggestedPort the port to use.  If 0 is chosen then the next free port will be used, the port
							chosen can be discovered via getSocketStatu()
     * @param tjwsProperties    properties to be passed into the TJWS server instance. Note that if
     *                          port is set in these properties they will be overwritten by suggestedPort
     * @param cblRequestAuthorization Specifies the authorization policy, can be NULL
     */
    public CBLListener(CBLServer server, int suggestedPort, Properties tjwsProperties, CBLRequestAuthorization cblRequestAuthorization) {
        this.server = server;
        tjwsProperties.put(Serve.ARG_PORT, suggestedPort);
        this.httpServer = new CBLHTTPServer(server, tjwsProperties, cblRequestAuthorization);
    }

    @Override
    public void run() {
        httpServer.serve();
    }

    public int start() {
        thread = new Thread(this);
        thread.start();
        return 0;
    }

    public void stop() {
        httpServer.notifyStop();
    }

    public void onServerThread(Runnable r) {
        ScheduledExecutorService workExecutor = server.getWorkExecutor();
        workExecutor.submit(r);
    }

    public CBLSocketStatus getSocketStatus() {
        return this.httpServer.getSocketStatus();
    }
}
