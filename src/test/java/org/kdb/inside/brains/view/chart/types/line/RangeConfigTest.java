package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.awt.*;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}