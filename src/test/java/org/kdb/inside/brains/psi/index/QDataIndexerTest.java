package org.kdb.inside.brains.psi.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class QDataIndexerTest {
    @Test
    void containsSetBackward() {
        checkBackward("set[`sdfdfasdf;10]");
        checkBackward(" set[`sdfdfasdf;10]");
        checkBackward(" set [`sdfdfasdf;10]");
        checkBackward(" set [ `sdfdfasdf;10]");
        checkBackward("  set  [  `sdfdfasdf;10]");
        checkBackward("\n  set\n  [\n  `sdfdfasdf;10]");
    }

    @Test
    void containsSetForward() {
        checkSetForward("`sdfdfasdf set");
        checkSetForward(" `sdfdfasdf   set");
        checkSetForward("\n `sdfdfasdf\n   set");
    }

    @Test
    void findIndexes() {
        assertArrayEquals(new int[]{4}, QDataIndexer.findAllOffsets("asd : 10;"));
        assertArrayEquals(new int[]{0}, QDataIndexer.findAllOffsets("`asd set 10;"));
        assertArrayEquals(new int[]{10, 14}, QDataIndexer.findAllOffsets(".asd.qwe[ `x ]: 10;"));
        assertArrayEquals(new int[]{10, 16, 20}, QDataIndexer.findAllOffsets(".asd.qwe[ `x ][ `y ]: 10;"));
    }

    void checkBackward(String s) {
        assertTrue(QDataIndexer.containsSetBackward(s, s.indexOf('`')));
    }

    void checkSetForward(String s) {
        final int startPosition = s.indexOf('`');
        assertTrue(QDataIndexer.containsSetForward(s, s.indexOf(' ', startPosition + 1)));
    }
}