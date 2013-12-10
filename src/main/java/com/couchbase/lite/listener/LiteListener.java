package com.couchbase.lite.listener;

import Acme.Serve.Serve;
import com.couchbase.lite.Manager;
import com.couchbase.lite.router.RequestAuthorization;
import com.couchbase.lite.router.URLStreamHandlerFactory;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

public class LiteListener implements Runnable {

    private Thread thread;
    private Manager manager;
    private LiteServer httpServer;
    public static final String TAG = "LiteListener";

    //static initializer to ensure that cblite:// URLs are handled properly
    {
        URLStreamHandlerFactory.registerSelfIgnoreError();
    }

    /**
     * LiteListener constructor
     *
     * @param port the suggested port to use. If 0 is specified then the next available port will be picked.
     */
    public LiteListener(Manager manager, int port) {
        this(manager, port, new Properties(), null);
    }

    /**
     *
     * @param manager 
     * @param port the port to use.  If 0 is chosen then the next free port will be used, the port
							chosen can be discovered via getSocketStatu()
     * @param tjwsProperties    properties to be passed into the TJWS server instance. Note that if
     *                          port is set in these properties they will be overwritten by suggestedPort
     * @param requestAuthorization Specifies the authorization policy, can be NULL
     */
    public LiteListener(Manager manager, int port, Properties tjwsProperties, RequestAuthorization requestAuthorization) {
        this.manager = manager;
        tjwsProperties.put(Serve.ARG_PORT, port);
        this.httpServer = new LiteServer(manager, tjwsProperties, requestAuthorization);
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
        ScheduledExecutorService workExecutor = manager.getWorkExecutor();
        workExecutor.submit(r);
    }

    public SocketStatus getSocketStatus() {
        return this.httpServer.getSocketStatus();
    }
}
