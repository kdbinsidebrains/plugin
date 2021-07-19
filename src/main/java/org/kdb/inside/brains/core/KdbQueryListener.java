package org.kdb.inside.brains.core;

public interface KdbQueryListener {
    default void queryStarted(InstanceConnection connection, KdbQuery query) {
    }

    default void queryFinished(InstanceConnection connection, KdbQuery query, KdbResult result) {
    }
}
