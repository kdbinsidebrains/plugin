package org.kdb.inside.brains.core;

public enum InstanceState {
    DISCONNECTED(true, false),
    CONNECTING(false, true),
    CONNECTED(false, true);

    private final boolean connectable;
    private final boolean disconnectable;

    InstanceState(boolean connectable, boolean disconnectable) {
        this.connectable = connectable;
        this.disconnectable = disconnectable;
    }

    public boolean isConnectable() {
        return connectable;
    }

    public boolean isDisconnectable() {
        return disconnectable;
    }
}
