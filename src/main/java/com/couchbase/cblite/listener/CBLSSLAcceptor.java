package com.couchbase.cblite.listener;

import Acme.Serve.SSLAcceptor;
import Acme.Serve.Serve;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Created by yarong on 11/1/13.
 */
public class CBLSSLAcceptor extends SSLAcceptor implements CBLAcceptor {

    /**
     * This class is based on SSLAcceptor from TJWS but in addition to adding the CBLAcceptor interface it
     * makes two other changes. If SSLAcceptor.ARG_PORT isn't specified but Serve.ARG_PORT is (and yes, they have
     * different values) then the Serve.ARG_PORT value will be used to set the SSLAcceptor.ARG_PORT value. Similarly
     * if SSLAcceptor.IFADDRESS isn't set but Serve.ARG_BINDADDRESS is then SSLACeeptor.IFADDRESS will be set with the
     * Serve.ARG_BINDADDRESS value.
     * @param inProperties
     * @param outProperties
     * @throws IOException
     */
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

        if ((inProperties.containsKey(this.ARG_IFADDRESS) == false) && (inProperties.containsKey(Serve.ARG_BINDADDRESS))) {
            inProperties.put(this.ARG_IFADDRESS, inProperties.get(Serve.ARG_BINDADDRESS));
        }

        super.init(inProperties, outProperties);
    }

    private ServerSocket getLocalSocket() {
        // There are race conditions where the server is being initialized on one thread while a
        // caller is on another thread. In that case we can end up with the acceptor having been
        // initialized but not the socket.
        while(this.socket == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e("CBLSSLAcceptor","getPort sleep somehow got interrupted", e);
                throw new RuntimeException("getPort sleep somehow got interrupted", e);
            }
        }

        return this.socket;
    }

    @Override
    public CBLSocketStatus getCBLSocketStatus() {
        return new CBLSocketStatus(getLocalSocket());
    }
}
