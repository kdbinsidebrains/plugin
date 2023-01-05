package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.JDOMException;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.view.chart.types.ChartType;
import org.kdb.inside.brains.view.chart.types.line.LineChartConfig;
import org.kdb.inside.brains.view.chart.types.line.LineChartConfigTest;
import org.kdb.inside.brains.view.chart.types.ohlc.OHLCChartConfig;
import org.kdb.inside.brains.view.chart.types.ohlc.OHLCChartConfigTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChartTemplatesServiceTest {
    public static final String MESSAGE = "<templates>\n" +
            "  <template name=\"t1\" quickAction=\"false\">\n" +
            "    <description>my template 1</description>\n" +
            "    <line-chart drawShapes=\"true\">\n" +
            "      <domain name=\"domain\" type=\"p\" />\n" +
            "      <series name=\"Series1\" type=\"LINE\" lowerMargin=\"5\" upperMargin=\"5\">\n" +
            "        <column name=\"r1\" type=\"f\" color=\"000000\" width=\"2.0\" />\n" +
            "        <column name=\"r2\" type=\"f\" color=\"000000\" width=\"4.5\" />\n" +
            "      </series>\n" +
            "      <series name=\"Series2\" type=\"AREA\" lowerMargin=\"5\" upperMargin=\"5\">\n" +
            "        <column name=\"r3\" type=\"i\" color=\"00ff00\" width=\"2.0\" />\n" +
            "      </series>\n" +
            "    </line-chart>\n" +
            "  </template>\n" +
            "  <template name=\"t2\" quickAction=\"true\">\n" +
            "    <ohlc-chart>\n" +
            "      <domain name=\"date\" type=\"p\" />\n" +
            "      <open name=\"open\" type=\"f\" />\n" +
            "      <high name=\"high\" type=\"f\" />\n" +
            "      <low name=\"low\" type=\"f\" />\n" +
            "      <close name=\"close\" type=\"f\" />\n" +
            "      <volume name=\"volume\" type=\"f\" />\n" +
            "    </ohlc-chart>\n" +
            "  </template>\n" +
            "</templates>";

    @Test
    void getState() {
        final LineChartConfig lineChart = LineChartConfigTest.createChartConfig();
        final OHLCChartConfig ohlcChart = OHLCChartConfigTest.createConfig();

        final ChartTemplate t1 = new ChartTemplate("t1", "my template 1", lineChart, false);
        final ChartTemplate t2 = new ChartTemplate("t2", null, ohlcChart, true);

        final ChartTemplatesService service = new ChartTemplatesService();
        service.setTemplates(List.of(t1));
        service.insertTemplate(t2);

        assertEquals(MESSAGE, JDOMUtil.write(service.getState()));
    }

    @Test
    void loadState() throws IOException, JDOMException {
        final ChartTemplatesService service = new ChartTemplatesService();
        service.loadState(JDOMUtil.load(MESSAGE));

        final List<ChartTemplate> templates = service.getTemplates();
        assertEquals(2, templates.size());

        assertTemplate(templates.get(0), "t1", "my template 1", ChartType.LINE);
        assertTemplate(templates.get(1), "t2", null, ChartType.OHLC);
    }

    void assertTemplate(ChartTemplate t, String name, String description, ChartType type) {
        assertEquals(name, t.getName());
        assertEquals(description, t.getDescription());
        assertEquals(type, t.getConfig().getType());
    }
}