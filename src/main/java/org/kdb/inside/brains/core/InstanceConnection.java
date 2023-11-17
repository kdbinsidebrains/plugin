package org.kdb.inside.brains.core;

import java.util.function.Consumer;

public interface InstanceConnection {
    KdbInstance getInstance();

    void connect();

    void disconnect();

    InstanceState connectAndWait();


    boolean isTemporal();


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

    default String getSymbol() {
        return getInstance().toSymbol();
    }

    default String getAddress() {
        return getInstance().toAddress();
    }

    default String getCanonicalName() {
        return getInstance().getCanonicalName();
    }

    default boolean isConnected() {
        return getState() == InstanceState.CONNECTED;
    }
}
