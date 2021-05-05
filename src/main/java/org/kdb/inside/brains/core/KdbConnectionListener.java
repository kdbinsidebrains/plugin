package org.kdb.inside.brains.core;

public interface KdbConnectionListener {
    default void connectionCreated(InstanceConnection connection) {
    }

    default void connectionRemoved(InstanceConnection connection) {
    }

    default void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
    }

    default void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
    }
}
