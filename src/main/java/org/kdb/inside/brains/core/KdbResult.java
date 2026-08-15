package org.kdb.inside.brains.core;

import java.time.LocalDateTime;

public class KdbResult {
    private final LocalDateTime time;

    private final long statedMillis;
    private final long statedNanos;

    private long finishedMillis;
    private long finishedNanos;

    private long size;
    private volatile String humanSize;

    private Object result;

    private static final String[] BYTE_UNITS = {"b", "Kb", "Mb", "Gb", "Tb", "Pb"};

    public KdbResult() {
        time = LocalDateTime.now();
        statedMillis = System.currentTimeMillis();
        statedNanos = System.nanoTime();
    }

    public KdbResult complete(Object result, long size) {
        if (finishedMillis != 0) {
            throw new IllegalStateException("Already finalized");
        }
        finishedNanos = System.nanoTime();
        finishedMillis = System.currentTimeMillis();
        this.result = result;
        this.size = size;
        return this;
    }

    public static KdbResult with(Object result, long size) {
        return new KdbResult().complete(result, size);
    }

    public long getSize() {
        return size;
    }

    public String getHumanSize() {
        if (humanSize == null) {
            humanSize = calculateHumanSize(size);
        }
        return humanSize;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getTimeAsTimestampString() {
        return time.toString().replace('T', 'D');
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

    static String calculateHumanSize(long size) {
        if (size <= 0) {
            return "0b";
        }

        int unit = 0;
        long value = size;

        while (value >= 1024 && unit < BYTE_UNITS.length - 1) {
            value >>= 10;
            unit++;
        }

        // Keep the current unit for values below 100.
        if (value < 100 || unit == BYTE_UNITS.length - 1) {
            return value + BYTE_UNITS[unit];
        }

        // 100+ → jump to the next unit.
        // value is now 100..1023, so the next unit is 0.1..1.0.
        unit++;
        long tenths = (value * 10 + 512) >> 10;

        return "0." + tenths + BYTE_UNITS[unit];
    }

    @Override
    public String toString() {
        return "KdbResult{" +
                "time=" + time +
                ", size=" + size +
                ", statedMillis=" + statedMillis +
                ", statedNanos=" + statedNanos +
                ", finishedMillis=" + finishedMillis +
                ", finishedNanos=" + finishedNanos +
                ", result=" + (result != null ? result.getClass() : "null") +
                '}';
    }
}
