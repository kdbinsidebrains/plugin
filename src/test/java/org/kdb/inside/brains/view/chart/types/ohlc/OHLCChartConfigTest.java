package org.kdb.inside.brains.view.chart.types.ohlc;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnConfig;
import org.kdb.inside.brains.view.chart.ColumnConfigTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OHLCChartConfigTest {
    public static final String MESSAGE = "<ohlc-chart>\n" +
            "  <date name=\"date\" type=\"p\" />\n" +
            "  <open name=\"open\" type=\"f\" />\n" +
            "  <high name=\"high\" type=\"f\" />\n" +
            "  <low name=\"low\" type=\"f\" />\n" +
            "  <close name=\"close\" type=\"f\" />\n" +
            "  <volume name=\"volume\" type=\"f\" />\n" +
            "</ohlc-chart>";

    @NotNull
    public static OHLCChartConfig createConfig() {
        final ColumnConfig date = new ColumnConfig("date", KdbType.TIMESTAMP);
        final ColumnConfig open = new ColumnConfig("open", KdbType.FLOAT);
        final ColumnConfig high = new ColumnConfig("high", KdbType.FLOAT);
        final ColumnConfig low = new ColumnConfig("low", KdbType.FLOAT);
        final ColumnConfig close = new ColumnConfig("close", KdbType.FLOAT);
        final ColumnConfig volume = new ColumnConfig("volume", KdbType.FLOAT);

        return new OHLCChartConfig(date, open, high, low, close, volume);
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
        ColumnConfigTest.assertColumn(c.getDomain(), "date", KdbType.TIMESTAMP);
        ColumnConfigTest.assertColumn(c.getOpenColumn(), "open", KdbType.FLOAT);
        ColumnConfigTest.assertColumn(c.getHighColumn(), "high", KdbType.FLOAT);
        ColumnConfigTest.assertColumn(c.getLowColumn(), "low", KdbType.FLOAT);
        ColumnConfigTest.assertColumn(c.getCloseColumn(), "close", KdbType.FLOAT);
        ColumnConfigTest.assertColumn(c.getVolumeColumn(), "volume", KdbType.FLOAT);
    }
}