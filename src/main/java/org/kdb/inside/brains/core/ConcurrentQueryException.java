package org.kdb.inside.brains.core;

public class ConcurrentQueryException extends Exception {
    public ConcurrentQueryException(String message) {
        super(message);
    }
}
