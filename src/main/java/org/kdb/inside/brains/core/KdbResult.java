package org.kdb.inside.brains.core;

public class KdbResult {
    private final long statedMillis;
    private final long statedNanos;

    private long finishedMillis;
    private long finishedNanos;

    private Object result;

    public KdbResult() {
        statedMillis = System.currentTimeMillis();
        statedNanos = System.nanoTime();
    }

    public KdbResult complete(Object result) {
        if (finishedMillis != 0) {
            throw new IllegalStateException("Already finalized");
        }
        finishedNanos = System.nanoTime();
        finishedMillis = System.currentTimeMillis();
        this.result = result;
        return this;
    }

    public boolean isError() {
        return result instanceof Exception;
    }

    public Object getObject() {
        return result;
    }

    public long getStatedMillis() {
        return statedMillis;
    }

    public long getFinishedMillis() {
        return finishedMillis;
    }

    public long getStatedNanos() {
        return statedNanos;
    }

    public long getFinishedNanos() {
        return finishedNanos;
    }

    public long getRoundtripNanos() {
        return finishedNanos - statedNanos;
    }

    public long getRoundtripMillis() {
        return finishedMillis - statedMillis;
    }
}
