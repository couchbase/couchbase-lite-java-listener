package com.couchbase.cblite.listener;

import android.util.Log;

import java.io.IOException;
import java.util.Map;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;

/**
 * Created by yarong on 11/1/13.
 */
public class CBLSSLAcceptor extends SSLAcceptor implements CBLAcceptor {
    @Override
    public int getPort() {
        // There are race conditions where the server is being initialized on one thread while a
        // caller is on another thread. In that case we can end up with the acceptor having been
        // initialized but not the socket.
        while(this.socket == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("CBLSSLAcceptor","getPort sleep somehow got interrupted", e);
            }
        }

        return this.socket.getLocalPort();
    }

    @Override
    public void init(Map inProperties, Map outProperties) throws IOException {
        if (inProperties.containsKey(this.ARG_PORT) == false) {
            // For some odd reason the SSL Acceptor uses a different value for ARG_PORT than server does,
            // rather than surface this to users we just copy the port value across.
            // For another odd reasons SSL Acceptor only accepts the port value if it is a string.
            // Which would make sense if in other acceptors Integers weren't used.
            Object serveArgPort = inProperties.get(Serve.ARG_PORT);
            if (serveArgPort != null) {
                String sslArgPort;
                if (serveArgPort instanceof String) {
                    sslArgPort = (String)serveArgPort;
                } else if (serveArgPort instanceof Integer) {
                    sslArgPort = serveArgPort.toString();
                } else {
                    throw new RuntimeException(this.ARG_PORT + "is set to a value that is not an integer or a string.");
                }
                inProperties.put(this.ARG_PORT, sslArgPort);
            }
        }

        super.init(inProperties, outProperties);
    }
}
