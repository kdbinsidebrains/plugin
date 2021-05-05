package org.kdb.inside.brains.ui;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.UIUtils;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UIUtilsTest {
    @Test
    void color() {
        final Color red = Color.RED;

        final String s = UIUtils.encodeColor(red);
        assertEquals("#ff0000", s);
        assertEquals(red, UIUtils.decodeColor(s));
    }
}