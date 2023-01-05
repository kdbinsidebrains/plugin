package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SeriesConfigTest {
    public static void assertSeries(SeriesConfig c, String name, SeriesType type, int lowerMargin, int upperMargin) {
        assertEquals(name, c.getName());
        assertEquals(type, c.getType());
        assertEquals(lowerMargin, c.getLowerMargin());
        assertEquals(upperMargin, c.getUpperMargin());
    }

    @Test
    void equals() {
        final SeriesConfig c1 = new SeriesConfig("name", SeriesType.LINE);
        final SeriesConfig c2 = new SeriesConfig("name", SeriesType.LINE);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        c2.setUpperMargin(12);
        assertNotEquals(c1, c2);
        assertNotEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void copy() {
        final SeriesConfig c1 = new SeriesConfig("name", SeriesType.LINE);
        assertEquals(c1, c1.copy());
        assertNotSame(c1, c1.copy());
    }

    @Test
    void store() {
        final SeriesConfig c = new SeriesConfig("name", SeriesType.LINE);
        assertEquals("<series name=\"name\" type=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final SeriesConfig c = SeriesConfig.restore(JDOMUtil.load("<series name=\"name\" type=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\" />"));
        assertSeries(c, "name", SeriesType.LINE, 5, 5);
    }
}