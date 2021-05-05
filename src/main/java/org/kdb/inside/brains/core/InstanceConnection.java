package org.kdb.inside.brains.core;

import java.util.function.Consumer;

public interface InstanceConnection {
    KdbInstance getInstance();

    void connect();

    void connectAndWait();

    void disconnect();


    InstanceState getState();

    long getStateChangeTime();


    Exception getDisconnectError();


    void cancelQuery();

    boolean isQueryCancelled();


    KdbQuery getQuery();

    KdbResult query(KdbQuery query) throws ConcurrentQueryException;

    void query(KdbQuery query, Consumer<KdbResult> handler) throws ConcurrentQueryException;


    default String getName() {
        return getInstance().getName();
    }

    default String getDetails() {
        return getInstance().toSymbol();
    }

    default String getCanonicalName() {
        return getInstance().getCanonicalName();
    }
}
