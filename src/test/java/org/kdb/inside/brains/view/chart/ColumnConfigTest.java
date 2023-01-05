package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void equals() {
        final ColumnConfig c1 = new ColumnConfig("name", KdbType.TIME);
        final ColumnConfig c2 = new ColumnConfig("name", KdbType.TIME);
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());

        final ColumnConfig c3 = new ColumnConfig("name1", KdbType.TIME);
        assertNotEquals(c1, c3);
        assertNotEquals(c1.hashCode(), c3.hashCode());

        final ColumnConfig c4 = new ColumnConfig("name", KdbType.TIMESTAMP);
        assertNotEquals(c1, c4);
        assertNotEquals(c1.hashCode(), c4.hashCode());
    }

    @Test
    void copy() {
        final ColumnConfig c1 = new ColumnConfig("name", KdbType.TIME);
        assertEquals(c1, ColumnConfig.copy(c1));
        assertNotSame(c1, ColumnConfig.copy(c1));
    }
}