package org.kdb.inside.brains.view.chart.types.ohlc;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnDefinition;
import org.kdb.inside.brains.view.chart.ColumnDefinitionTest;

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
        final ColumnDefinition date = new ColumnDefinition("date", KdbType.TIMESTAMP);
        final ColumnDefinition open = new ColumnDefinition("open", KdbType.FLOAT);
        final ColumnDefinition high = new ColumnDefinition("high", KdbType.FLOAT);
        final ColumnDefinition low = new ColumnDefinition("low", KdbType.FLOAT);
        final ColumnDefinition close = new ColumnDefinition("close", KdbType.FLOAT);
        final ColumnDefinition volume = new ColumnDefinition("volume", KdbType.FLOAT);

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
        ColumnDefinitionTest.assertColumn(c.domain(), "date", KdbType.TIMESTAMP);
        ColumnDefinitionTest.assertColumn(c.openColumn(), "open", KdbType.FLOAT);
        ColumnDefinitionTest.assertColumn(c.highColumn(), "high", KdbType.FLOAT);
        ColumnDefinitionTest.assertColumn(c.lowColumn(), "low", KdbType.FLOAT);
        ColumnDefinitionTest.assertColumn(c.closeColumn(), "close", KdbType.FLOAT);
        ColumnDefinitionTest.assertColumn(c.volumeColumn(), "volume", KdbType.FLOAT);
    }
}