package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnDefinition;
import org.kdb.inside.brains.view.chart.ColumnDefinitionTest;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LineChartConfigTest {
    public static final String MESSAGE = """
            <line-chart drawShapes="true">
              <domain name="domain" type="p" />
              <series>
                <series name="Series1" style="LINE" lowerMargin="5" upperMargin="5">
                  <column name="r1" type="f" operation="MAX" />
                  <column name="r2" type="f" operation="SUM" />
                </series>
                <series name="Series2" style="AREA" lowerMargin="5" upperMargin="5">
                  <column name="r3" type="i" operation="COUNT" />
                </series>
              </series>
              <expansions>
                <column name="e1" type="s" />
                <column name="e2" type="c" />
              </expansions>
            </line-chart>
            """.trim();

    @Test
    void store() {
        final LineChartConfig c = createChartConfig();

        final Element store = c.store();
        assertEquals(MESSAGE, JDOMUtil.write(store));
    }

    @NotNull
    public static LineChartConfig createChartConfig() {
        final SeriesDefinition s1 = new SeriesDefinition("Series1", SeriesStyle.LINE);
        final SeriesDefinition s2 = new SeriesDefinition("Series2", SeriesStyle.AREA);

        final ColumnDefinition domain = new ColumnDefinition("domain", KdbType.TIMESTAMP);

        final ValuesDefinition r1 = new ValuesDefinition(new ColumnDefinition("r1", KdbType.FLOAT), s1, Operation.MAX);

        final ValuesDefinition r2 = new ValuesDefinition(new ColumnDefinition("r2", KdbType.FLOAT), s1, Operation.SUM);

        final ValuesDefinition r3 = new ValuesDefinition(new ColumnDefinition("r3", KdbType.INT), s2, Operation.COUNT);

        final ColumnDefinition e1 = new ColumnDefinition("e1", KdbType.SYMBOL);

        final ColumnDefinition e2 = new ColumnDefinition("e2", KdbType.CHAR);

        return new LineChartConfig(domain, List.of(r1, r2, r3), List.of(e1, e2), true);
    }

    @Test
    void restore() throws IOException, JDOMException {
        final LineChartConfig c = LineChartConfig.restore(JDOMUtil.load(MESSAGE));
        assertTrue(c.isDrawShapes());
        assertEquals(ChartType.LINE, c.getChartType());

        ColumnDefinitionTest.assertColumn(c.domain(), "domain", KdbType.TIMESTAMP);

        assertEquals(3, c.values().size());
        ValuesDefinitionTest.assertRange(c.values().get(0), "r1", KdbType.FLOAT, Operation.MAX, "Series1");
        SeriesDefinitionTest.assertSeries(c.values().get(0).series(), "Series1", SeriesStyle.LINE, 5, 5);

        ValuesDefinitionTest.assertRange(c.values().get(1), "r2", KdbType.FLOAT, Operation.SUM, "Series1");
        SeriesDefinitionTest.assertSeries(c.values().get(1).series(), "Series1", SeriesStyle.LINE, 5, 5);

        ValuesDefinitionTest.assertRange(c.values().get(2), "r3", KdbType.INT, Operation.COUNT, "Series2");
        SeriesDefinitionTest.assertSeries(c.values().get(2).series(), "Series2", SeriesStyle.AREA, 5, 5);

        assertSame(c.values().get(0).series(), c.values().get(1).series());

        final List<ColumnDefinition> expansions = c.expansions();
        assertEquals(2, expansions.size());

        ColumnDefinitionTest.assertColumn(expansions.get(0), "e1", KdbType.SYMBOL);
        ColumnDefinitionTest.assertColumn(expansions.get(1), "e2", KdbType.CHAR);
    }
}