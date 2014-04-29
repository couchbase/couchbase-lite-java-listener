package com.couchbase.lite.listener;

import com.couchbase.lite.Manager;

import java.util.Properties;

import Acme.Serve.Serve;

@SuppressWarnings("serial")
public class LiteServer extends Serve {

    public static final String CBLServer_KEY = "CBLServerInternal";
    public static final String CBL_URI_SCHEME = "cblite://";

    private Properties props;
    private Manager manager;
    private LiteListener listener;

    // if this is non-null, then users must present BasicAuth
    // credential in every request which matches up with allowedCredentials,
    // or else the request will be refused.
    // REF: https://github.com/couchbase/couchbase-lite-java-listener/issues/35
    private Credentials allowedCredentials;

    public LiteServer() {
        props = new Properties();
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    public void setListener(LiteListener listener) {
        this.listener = listener;
    }

    public void setAllowedCredentials(Credentials allowedCredentials) {
        this.allowedCredentials = allowedCredentials;
    }

    public void setPort(int port) {
        props.put("port", port);
    }

    @Override
    public int serve() {
        //pass our custom properties in
        this.arguments = props;

        //pass in the CBLServerInternal to the servlet
        LiteServlet servlet = new LiteServlet();
        servlet.setManager(manager);
        servlet.setListener(listener);
        if (allowedCredentials != null) {
            servlet.setAllowedCredentials(allowedCredentials);
        }

        this.addServlet("/", servlet);
        return super.serve();
    }

}
