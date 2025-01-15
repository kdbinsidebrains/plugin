package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeriesDefinitionTest {
    public static void assertSeries(SeriesDefinition c, String name, SeriesStyle type, int lowerMargin, int upperMargin) {
        assertEquals(name, c.name());
        assertEquals(type, c.style());
        assertEquals(lowerMargin, c.lowerMargin());
        assertEquals(upperMargin, c.upperMargin());
    }

    @Test
    void store() {
        final SeriesDefinition c = new SeriesDefinition("name", SeriesStyle.LINE);
        assertEquals("<series name=\"name\" style=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final SeriesDefinition c = SeriesDefinition.restore(JDOMUtil.load("<series name=\"name\" style=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\" />"));
        assertSeries(c, "name", SeriesStyle.LINE, 5, 5);
    }
}