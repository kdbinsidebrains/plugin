package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeriesConfigTest {
    public static void assertSeries(SeriesConfig c, String name, SeriesType type, int lowerMargin, int upperMargin) {
        assertEquals(name, c.getName());
        assertEquals(type, c.getType());
        assertEquals(lowerMargin, c.getLowerMargin());
        assertEquals(upperMargin, c.getUpperMargin());
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