package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.awt.*;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RangeConfigTest {
    public static void assertRange(RangeConfig c, String name, KdbType type, Color color, float width) {
        assertEquals(name, c.getName());
        assertEquals(type, c.getType());
        assertEquals(color, c.getColor());
        assertEquals(width, c.getWidth());
    }

    @Test
    void store() {
        final RangeConfig c = new RangeConfig("name", KdbType.FLOAT, Color.BLUE);
        c.setWidth(56.3f);
        assertEquals("<column name=\"name\" type=\"f\" color=\"0000ff\" width=\"56.3\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final RangeConfig c = RangeConfig.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" color=\"0000ff\" width=\"56.3\" />"));
        assertRange(c, "name", KdbType.FLOAT, Color.BLUE, 56.3f);
    }

    @Test
    void equals() {
        final RangeConfig c1 = new RangeConfig("name", KdbType.TIME, Color.BLACK);
        final RangeConfig c2 = new RangeConfig("name", KdbType.TIME, Color.BLACK);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        final RangeConfig c3 = new RangeConfig("name1", KdbType.TIME, Color.BLUE);
        assertNotEquals(c1, c3);
        assertNotEquals(c1.hashCode(), c3.hashCode());

        final RangeConfig c4 = new RangeConfig("name", KdbType.TIMESTAMP, Color.BLACK);
        c4.setWidth(13);
        assertNotEquals(c1, c4);
        assertNotEquals(c1.hashCode(), c4.hashCode());
    }
}