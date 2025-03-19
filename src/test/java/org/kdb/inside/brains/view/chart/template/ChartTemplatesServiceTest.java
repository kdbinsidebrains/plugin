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
    public static final String MESSAGE = """
            <templates>
              <line-chart drawShapes="true" name="t1" quickAction="false">
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
                <description>my template 1</description>
              </line-chart>
              <ohlc-chart name="t2" quickAction="true">
                <domain name="date" type="p" />
                <open name="open" type="f" />
                <high name="high" type="f" />
                <low name="low" type="f" />
                <close name="close" type="f" />
                <volume name="volume" type="f" />
              </ohlc-chart>
            </templates>
            """.trim();

    public static final String OLD_MESSAGE_1 = """
            <templates>
                <template name="test" quickAction="true">
                  <description>deasdff</description>
                  <line-chart drawShapes="false">
                    <domain name="time" type="d" />
                    <series>
                      <series name="Size" style="BAR" lowerMargin="5" upperMargin="5">
                        <column name="size" type="f" operation="SUM" />
                      </series>
                      <series name="Value" style="LINE" lowerMargin="5" upperMargin="5">
                        <column name="price" type="f" operation="SUM" />
                      </series>
                    </series>
                    <expansions>
                      <column name="side" type="c" />
                    </expansions>
                  </line-chart>
                </template>
            </templates>
            """;

    public static final String OLD_MESSAGE_2 = """
            <templates>
                <template name="test">
                  <line-chart drawShapes="false">
                    <domain name="time" type="d" />
                    <series>
                      <series name="Size" style="BAR" lowerMargin="5" upperMargin="5">
                        <column name="size" type="f" operation="SUM" />
                      </series>
                      <series name="Value" style="LINE" lowerMargin="5" upperMargin="5">
                        <column name="price" type="f" operation="SUM" />
                      </series>
                    </series>
                    <expansions>
                      <column name="side" type="c" />
                    </expansions>
                  </line-chart>
                </template>
            </templates>
            """;

    @Test
    void getState() {
        final LineChartConfig lineChart = LineChartConfigTest.createChartConfig();
        final OHLCChartConfig ohlcChart = OHLCChartConfigTest.createConfig();

        final ChartTemplate t1 = new ChartTemplate("t1", "my template 1", lineChart, false);
        final ChartTemplate t2 = new ChartTemplate("t2", null, ohlcChart, true);

        final ChartTemplatesService service = new ChartTemplatesService();
        service.setTemplates(List.of(t1));
        service.addTemplate(t2);

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

    @Test
    void loadOldState() throws IOException, JDOMException {
        final ChartTemplatesService service = new ChartTemplatesService();

        service.loadState(JDOMUtil.load(OLD_MESSAGE_1));
        final List<ChartTemplate> t1 = service.getTemplates();
        assertEquals(1, t1.size());
        assertTemplate(t1.get(0), "test", "deasdff", ChartType.LINE);

        service.loadState(JDOMUtil.load(OLD_MESSAGE_2));
        final List<ChartTemplate> t2 = service.getTemplates();
        assertEquals(1, t2.size());
        assertTemplate(t2.get(0), "test", null, ChartType.LINE);
    }

    void assertTemplate(ChartTemplate t, String name, String description, ChartType type) {
        assertEquals(name, t.getName());
        assertEquals(description, t.getDescription());
        assertEquals(type, t.getConfig().getChartType());
    }
}