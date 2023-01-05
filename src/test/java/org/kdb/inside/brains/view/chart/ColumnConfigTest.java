package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColumnConfigTest {
    public static void assertColumn(ColumnConfig c, String name, KdbType type) {
        assertEquals(name, c.getName());
        assertEquals(type, c.getType());
    }

    @Test
    void store() {
        final ColumnConfig c = new ColumnConfig("name", KdbType.FLOAT);
        assertEquals("<column name=\"name\" type=\"f\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final ColumnConfig c = ColumnConfig.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" />"));
        assertColumn(c, "name", KdbType.FLOAT);
    }
}