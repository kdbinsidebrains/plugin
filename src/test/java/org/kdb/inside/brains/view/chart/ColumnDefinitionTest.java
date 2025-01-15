package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColumnDefinitionTest {
    public static void assertColumn(ColumnDefinition c, String name, KdbType type) {
        assertEquals(name, c.name());
        assertEquals(type, c.type());
    }

    @Test
    void store() {
        final ColumnDefinition c = new ColumnDefinition("name", KdbType.FLOAT);
        assertEquals("<column name=\"name\" type=\"f\" />", JDOMUtil.write(c.store()));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final ColumnDefinition c = ColumnDefinition.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" />"));
        assertColumn(c, "name", KdbType.FLOAT);
    }
}