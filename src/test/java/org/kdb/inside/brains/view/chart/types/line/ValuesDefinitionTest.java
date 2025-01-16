package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnDefinition;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValuesDefinitionTest {
    public static void assertRange(ValuesDefinition c, String name, KdbType type, Operation operation, String series) {
        assertEquals(name, c.column().name());
        assertEquals(type, c.column().type());
        assertEquals(operation, c.operation());
        assertEquals(series, c.series().name());
    }

    @Test
    public void store() {
        final ValuesDefinition c = new ValuesDefinition(new ColumnDefinition("name", KdbType.FLOAT), null, Operation.MAX);
        assertEquals("<column name=\"name\" type=\"f\" operation=\"MAX\" />", JDOMUtil.write(c.store()));
    }

    @Test
    public void restore() throws IOException, JDOMException {
        final SeriesDefinition s = new SeriesDefinition("a", SeriesStyle.AREA);

        final ValuesDefinition c1 = ValuesDefinition.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" />"), s);
        assertRange(c1, "name", KdbType.FLOAT, Operation.SUM, s.name());

        final ValuesDefinition c2 = ValuesDefinition.restore(JDOMUtil.load("<column name=\"name\" type=\"f\" operation=\"MAX\"/>"), s);
        assertRange(c2, "name", KdbType.FLOAT, Operation.MAX, s.name());
    }
}