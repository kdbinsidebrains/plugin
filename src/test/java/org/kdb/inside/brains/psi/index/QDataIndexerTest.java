package org.kdb.inside.brains.psi.index;

import org.junit.jupiter.api.Test;

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

    void checkBackward(String s) {
        assertTrue(QDataIndexer.containsSetBackward(s, s.indexOf('`')));
    }

    void checkSetForward(String s) {
        final int startPosition = s.indexOf('`');
        assertTrue(QDataIndexer.containsSetForward(s, s.indexOf(' ', startPosition + 1)));
    }
}