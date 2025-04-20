package org.kdb.inside.brains.view.chart.types.ohlc;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartColumn;
import org.kdb.inside.brains.view.chart.ChartColumnTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OHLCChartConfigTest {
    public static final String MESSAGE = "<ohlc-chart>\n" +
            "  <domain name=\"date\" type=\"p\" />\n" +
            "  <open name=\"open\" type=\"f\" />\n" +
            "  <high name=\"high\" type=\"f\" />\n" +
            "  <low name=\"low\" type=\"f\" />\n" +
            "  <close name=\"close\" type=\"f\" />\n" +
            "  <volume name=\"volume\" type=\"f\" />\n" +
            "</ohlc-chart>";

    @NotNull
    public static OHLCChartConfig createConfig() {
        final ChartColumn date = new ChartColumn("date", KdbType.TIMESTAMP);
        final ChartColumn open = new ChartColumn("open", KdbType.FLOAT);
        final ChartColumn high = new ChartColumn("high", KdbType.FLOAT);
        final ChartColumn low = new ChartColumn("low", KdbType.FLOAT);
        final ChartColumn close = new ChartColumn("close", KdbType.FLOAT);
        final ChartColumn volume = new ChartColumn("volume", KdbType.FLOAT);

        return new OHLCChartConfig(date, open, high, low, close, volume);
    }

    @Test
    void equals() {
        final OHLCChartConfig chartConfig = createConfig();
        assertEquals(chartConfig, createConfig());
        assertEquals(chartConfig.hashCode(), createConfig().hashCode());
    }

    @Test
    void store() {
        final OHLCChartConfig c = createConfig();

        final Element store = c.store();
        assertEquals(MESSAGE, JDOMUtil.write(store));
    }

    @Test
    void restore() throws IOException, JDOMException {
        final OHLCChartConfig c = OHLCChartConfig.restore(JDOMUtil.load(MESSAGE));
        ChartColumnTest.assertColumn(c.domain(), "date", KdbType.TIMESTAMP);
        ChartColumnTest.assertColumn(c.openColumn(), "open", KdbType.FLOAT);
        ChartColumnTest.assertColumn(c.highColumn(), "high", KdbType.FLOAT);
        ChartColumnTest.assertColumn(c.lowColumn(), "low", KdbType.FLOAT);
        ChartColumnTest.assertColumn(c.closeColumn(), "close", KdbType.FLOAT);
        ChartColumnTest.assertColumn(c.volumeColumn(), "volume", KdbType.FLOAT);
    }
}