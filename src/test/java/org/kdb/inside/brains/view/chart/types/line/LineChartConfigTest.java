package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnConfig;
import org.kdb.inside.brains.view.chart.ColumnConfigTest;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LineChartConfigTest {
    public static final String MESSAGE = "<line-chart drawShapes=\"true\">\n" +
            "  <domain name=\"domain\" type=\"p\" />\n" +
            "  <series name=\"Series1\" type=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\">\n" +
            "    <column name=\"r1\" type=\"f\" color=\"000000\" width=\"2.0\" />\n" +
            "    <column name=\"r2\" type=\"f\" color=\"000000\" width=\"4.5\" />\n" +
            "  </series>\n" +
            "  <series name=\"Series2\" type=\"AREA\" lowerMargin=\"5\" upperMargin=\"5\">\n" +
            "    <column name=\"r3\" type=\"i\" color=\"00ff00\" width=\"2.0\" />\n" +
            "  </series>\n" +
            "</line-chart>";

    @NotNull
    public static LineChartConfig createChartConfig() {
        final SeriesConfig s1 = new SeriesConfig("Series1", SeriesType.LINE);
        final SeriesConfig s2 = new SeriesConfig("Series2", SeriesType.AREA);

        final ColumnConfig domain = new ColumnConfig("domain", KdbType.TIMESTAMP);

        final RangeConfig r1 = new RangeConfig("r1", KdbType.FLOAT, Color.BLACK);
        r1.setSeries(s1);

        final RangeConfig r2 = new RangeConfig("r2", KdbType.FLOAT, Color.BLACK);
        r2.setWidth(4.5f);
        r2.setSeries(s1);

        final RangeConfig r3 = new RangeConfig("r3", KdbType.INT, Color.GREEN);
        r3.setSeries(s2);

        return new LineChartConfig(domain, List.of(r1, r2, r3), true);
    }

    @Test
    void store() {
        final LineChartConfig c = createChartConfig();

        final Element store = c.store();
        assertEquals(MESSAGE, JDOMUtil.write(store));
    }

    @Test
    void copy() {
        final LineChartConfig chartConfig = createChartConfig();
        assertEquals(chartConfig, chartConfig.copy());
    }

    @Test
    void equals() {
        final LineChartConfig chartConfig = createChartConfig();
        assertEquals(chartConfig, createChartConfig());
        assertEquals(chartConfig.hashCode(), createChartConfig().hashCode());
    }

    @Test
    void restore() throws IOException, JDOMException {
        final LineChartConfig c = LineChartConfig.restore(JDOMUtil.load(MESSAGE));
        assertTrue(c.isDrawShapes());
        assertEquals(ChartType.LINE, c.getType());

        ColumnConfigTest.assertColumn(c.getDomain(), "domain", KdbType.TIMESTAMP);

        assertEquals(3, c.getRanges().size());
        RangeConfigTest.assertRange(c.getRanges().get(0), "r1", KdbType.FLOAT, Color.BLACK, 2.0f);
        SeriesConfigTest.assertSeries(c.getRanges().get(0).getSeries(), "Series1", SeriesType.LINE, 5, 5);

        RangeConfigTest.assertRange(c.getRanges().get(1), "r2", KdbType.FLOAT, Color.BLACK, 4.5f);
        SeriesConfigTest.assertSeries(c.getRanges().get(1).getSeries(), "Series1", SeriesType.LINE, 5, 5);

        RangeConfigTest.assertRange(c.getRanges().get(2), "r3", KdbType.INT, Color.GREEN, 2.0f);
        SeriesConfigTest.assertSeries(c.getRanges().get(2).getSeries(), "Series2", SeriesType.AREA, 5, 5);

        assertSame(c.getRanges().get(0).getSeries(), c.getRanges().get(1).getSeries());
    }
}