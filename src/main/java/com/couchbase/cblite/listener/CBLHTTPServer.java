package com.couchbase.cblite.listener;

import android.util.Log;

import java.util.Properties;

import Acme.Serve.Serve;

import com.couchbase.cblite.CBLServer;
import com.couchbase.cblite.router.CBLRequestAuthorization;

@SuppressWarnings("serial")
public class CBLHTTPServer extends Serve {

    public static final String CBLServer_KEY = "CBLServer";

    private final Properties props;
    private final CBLServer server;
    private final CBLRequestAuthorization cblRequestAuthorization;

    /**
     * Creates an instance of CBLHTTPServer with the server, listener & TJWS properties.
     * @param server
     * @param tjwsProperties At a minimum ARG_PORT has to be set to specify what port the server is to run on, 0 can be used to tell the server to pick the next available port.
     * @param cblRequestAuthorization This can be null if no special authorization policy is to be used.
     */
    public CBLHTTPServer(CBLServer server, Properties tjwsProperties, CBLRequestAuthorization cblRequestAuthorization) {
        this.server = server;
        props = tjwsProperties;
        this.cblRequestAuthorization = cblRequestAuthorization;
        if (props.containsKey(Serve.ARG_ACCEPTOR_CLASS) == false) {
            props.setProperty(Serve.ARG_ACCEPTOR_CLASS, "com.couchbase.cblite.listener.CBLSimpleAcceptor");
        }
    }

    public int getListenPort() {
        // There are race conditions where the server is being initialized on one thread while a
        // caller is on another thread. In that case we can end up with acceptor being null because
        // initialization hasn't completed yet.
        while(acceptor == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("CBLHTTPServer","getListenPort sleep somehow got interrupted", e);
            }
        }

        if (acceptor instanceof CBLAcceptor) {
            return ((CBLAcceptor)acceptor).getPort();
        }

        Log.e("CBLHTTPServer","we were asked for port on an acceptor that doesn't implement CBLAcceptor interface.");
        throw new RuntimeException("getListenPort is only supported on TJWS acceptors that support the CBLAcceptor interface.");
    }

    @Override
    public int serve() {
        //pass our custom properties in
        this.arguments = props;

        //pass in the CBLServer to the servlet
        CBLHTTPServlet servlet = new CBLHTTPServlet(server, cblRequestAuthorization);

        this.addServlet("/", servlet);
        return super.serve();
    }
}
