package com.couchbase.cblite.listener;

import java.util.Properties;

import Acme.Serve.Serve;

import com.couchbase.cblite.internal.CBLServerInternal;

@SuppressWarnings("serial")
public class CBLHTTPServer extends Serve {

    public static final String CBLServer_KEY = "CBLServerInternal";

    private Properties props;
    private CBLServerInternal server;
    private CBLListener listener;

    public CBLHTTPServer() {
        props = new Properties();
    }

    public void setServer(CBLServerInternal server) {
        this.server = server;
    }

    public void setListener(CBLListener listener) {
        this.listener = listener;
    }

    public void setPort(int port) {
        props.put("port", port);
    }

    @Override
    public int serve() {
        //pass our custom properties in
        this.arguments = props;

        //pass in the CBLServerInternal to the servlet
        CBLHTTPServlet servlet = new CBLHTTPServlet();
        servlet.setServer(server);
        servlet.setListener(listener);

        this.addServlet("/", servlet);
        return super.serve();
    }

}
