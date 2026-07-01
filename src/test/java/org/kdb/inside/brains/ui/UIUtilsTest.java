package org.kdb.inside.brains.ui;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.UIUtils;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kdb.inside.brains.UIUtils.replaceSystemProperties;

class UIUtilsTest {
    @Test
    void color() {
        final Color red = Color.RED;

        final String s = UIUtils.encodeColor(red);
        assertEquals("#ff0000", s);
        assertEquals(red, UIUtils.decodeColor(s));
    }

    @Test
    void test_replaceSystemProperties() {
        assertEquals("Hello World", replaceSystemProperties("Hello World"));

        System.setProperty("prop1", "one");
        System.setProperty("prop2", "$2\\temp");
        try {
            assertEquals("one", replaceSystemProperties("${prop1}"));
            assertEquals("one/one", replaceSystemProperties("${prop1}/${prop1}"));
            assertEquals("one-", replaceSystemProperties("${prop1}-${missing}"));
            assertEquals("$2\\temp", replaceSystemProperties("${prop2}"));
            assertEquals("one-$2\\temp", replaceSystemProperties("${prop1}-${prop2}"));
        } finally {
            System.clearProperty("prop1");
            System.clearProperty("prop2");
        }
    }
}