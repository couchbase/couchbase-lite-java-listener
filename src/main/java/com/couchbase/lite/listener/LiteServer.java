package com.couchbase.lite.listener;

import Acme.Serve.Serve;
import com.couchbase.lite.Manager;
import com.couchbase.lite.router.RequestAuthorization;
import com.couchbase.lite.util.Log;

import java.util.Properties;

@SuppressWarnings("serial")
public class LiteServer extends Serve {

    public static final String CBLServer_KEY = "CBLServerInternal";
    public static final String CBL_URI_SCHEME = "cblite://";
    public static final String Default_Acceptor = "com.couchbase.lite.listener.SimpleAcceptor";

    private Properties props;
    private Manager manager;
    private LiteListener listener;
    private final RequestAuthorization requestAuthorization;

    /**
     * Creates an instance of LiteServer with the server, listener & TJWS properties.
     * @param manager
     * @param tjwsProperties At a minimum ARG_PORT has to be set to specify what port the server is to run on, 0 can be used to tell the server to pick the next available port.
     * @param requestAuthorization This can be null if no special authorization policy is to be used.
     */
    public LiteServer(Manager manager, Properties tjwsProperties, RequestAuthorization requestAuthorization) {
        this.manager = manager;
        props = tjwsProperties;
        this.requestAuthorization = requestAuthorization;
        if (props.containsKey(Serve.ARG_ACCEPTOR_CLASS) == false) {
            props.setProperty(Serve.ARG_ACCEPTOR_CLASS, Default_Acceptor);
        }
    }

    public SocketStatus getSocketStatus() {
        // There are race conditions where the server is being initialized on one thread while a
        // caller is on another thread. In that case we can end up with acceptor being null because
        // initialization hasn't completed yet.
        while(acceptor == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("CBLHTTPServer", "getSocketStatus sleep somehow got interrupted", e);
            }
        }

        if (acceptor instanceof LiteAcceptor) {
            return ((LiteAcceptor)acceptor).getSocketStatus();
        }

        Log.e("CBLHTTPServer","we were asked for socket status on an acceptor that doesn't implement SocketStatus interface.");
        throw new RuntimeException("getSocketStatus is only supported on TJWS acceptors that support the SocketStatus interface.");
    }

    @Override
    public int serve() {
        //pass our custom properties in
        this.arguments = props;

        //pass in the CBLServerInternal to the servlet
        LiteServlet servlet = new LiteServlet(manager, requestAuthorization);

        this.addServlet("/", servlet);
        return super.serve();
    }

}
