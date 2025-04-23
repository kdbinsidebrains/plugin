package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChartColumnTest {
    public static void assertColumn(ChartColumn c, String name, KdbType type) {
        assertEquals(name, c.name());
        assertEquals(type, c.type());
    }

    @Test
    void store() {
        final ChartColumn c = new ChartColumn("name", KdbType.FLOAT);
        assertEquals("<column name=\"name\" type=\"f\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final ChartColumn c = ChartColumn.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" />"));
        assertColumn(c, "name", KdbType.FLOAT);
    }
}