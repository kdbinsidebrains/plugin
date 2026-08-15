package org.kdb.inside.brains.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KdbResultTest {
    @Test
    void formatBytes() {
        assertEquals("3b", KdbResult.calculateHumanSize(3));
        assertEquals("99b", KdbResult.calculateHumanSize(99));
        assertEquals("1Kb", KdbResult.calculateHumanSize(1024));
        assertEquals("99Kb", KdbResult.calculateHumanSize(99 * 1024L));
        assertEquals("0.1Mb", KdbResult.calculateHumanSize(100 * 1024L));
        assertEquals("0.5Mb", KdbResult.calculateHumanSize(512 * 1024L));
        assertEquals("1Mb", KdbResult.calculateHumanSize(1024 * 1024L));
        assertEquals("99Mb", KdbResult.calculateHumanSize(99 * 1024L * 1024));
        assertEquals("0.1Gb", KdbResult.calculateHumanSize(100 * 1024L * 1024));
        assertEquals("0.5Gb", KdbResult.calculateHumanSize(512 * 1024L * 1024));
        assertEquals("1Gb", KdbResult.calculateHumanSize(1024 * 1024L * 1024));
        assertEquals("99Gb", KdbResult.calculateHumanSize(99 * 1024L * 1024 * 1024));
    }
}