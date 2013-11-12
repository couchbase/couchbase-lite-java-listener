package com.couchbase.cblite.listener;

import Acme.Serve.Serve;

/**
 * Created by yarong on 11/1/13.
 */
public interface CBLAcceptor extends Serve.Acceptor {
    /**
     * Returns the port the acceptor has bound to.
     * @return
     */
    int getPort();
}
