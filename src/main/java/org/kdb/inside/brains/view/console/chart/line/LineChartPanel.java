package org.kdb.inside.brains.view.console.chart.line;

import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kdb.inside.brains.view.console.chart.BaseChartPanel;
import org.kdb.inside.brains.view.console.chart.ChartDataProvider;

import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LineChartPanel extends BaseChartPanel {
    public LineChartPanel(ChartConfig config, ChartDataProvider dataProvider) {
        super(createChart(config, dataProvider));
    }

    private static JFreeChart createChart(ChartConfig config, ChartDataProvider dataProvider) {
        final AxisConfig domain = config.getDomainValues();
        final Map<String, List<AxisConfig>> ranges = config.dataset();

        final XYDataset[] datasets = AxisConfig.isTemporalType(domain.getType()) ? createTimeDatasets(domain, ranges, dataProvider) : createNumberDatasets(domain, ranges, dataProvider);

        final JFreeChart chart = ChartFactory.createTimeSeriesChart(null, domain.getName(), "", null, true, true, false);

        int i = 0;
        final XYPlot plot = chart.getXYPlot();

        for (Map.Entry<String, List<AxisConfig>> entry : ranges.entrySet()) {
            plot.setDataset(i, datasets[i]);
            plot.setRenderer(i, createRenderer(entry.getValue()));
            plot.setRangeAxis(i, applyAxisColorSchema(new NumberAxis(entry.getKey())));
            plot.setRangeAxisLocation(i, i % 2 == 0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
            plot.mapDatasetToRangeAxis(i, i);
            i++;
        }
        return chart;
    }

    @NotNull
    private static XYItemRenderer createRenderer(List<AxisConfig> configs) {
        int c = 0;
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (AxisConfig column : configs) {
            renderer.setSeriesPaint(c, column.getColor());
            renderer.setSeriesStroke(c, new BasicStroke(2.0f));
            renderer.setSeriesLinesVisible(c, column.getLineStyle().isLineVisible());
            renderer.setSeriesShapesVisible(c, column.getLineStyle().isShapeVisible());
            c++;
        }
        return renderer;
    }

    private static XYDataset[] createNumberDatasets(AxisConfig domainCfg, Map<String, List<AxisConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final Number[] domain = createNumberDomain(domainCfg, dataProvider);
        final XYDataset[] res = new XYDataset[datasets.size()];
        for (Map.Entry<String, List<AxisConfig>> entry : datasets.entrySet()) {
            final XYSeriesCollection series = new XYSeriesCollection();
            for (AxisConfig column : entry.getValue()) {
                final XYSeries s = new XYSeries(column.getName());
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], (Number) dataProvider.getValueAt(j, column.getIndex()));
                }
                series.addSeries(s);
            }
            res[i++] = series;
        }
        return res;
    }

    private static XYDataset[] createTimeDatasets(AxisConfig domainCfg, Map<String, List<AxisConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final RegularTimePeriod[] domain = createTimeDomain(domainCfg, dataProvider);
        final XYDataset[] res = new XYDataset[datasets.size()];
        for (Map.Entry<String, List<AxisConfig>> entry : datasets.entrySet()) {
            final TimeSeriesCollection series = new TimeSeriesCollection();
            for (AxisConfig column : entry.getValue()) {
                final TimeSeries s = new TimeSeries(column.getName());
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], (Number) dataProvider.getValueAt(j, column.getIndex()));
                }
                series.addSeries(s);
            }
            res[i++] = series;
        }
        return res;
    }

    private static Number[] createNumberDomain(AxisConfig domain, ChartDataProvider dataProvider) {
        final int index = domain.getIndex();
        final int rowsCount = dataProvider.getRowCount();
        final Number[] res = new Number[rowsCount];
        for (int row = 0; row < rowsCount; row++) {
            res[row] = (Number) dataProvider.getValueAt(row, index);
        }
        return res;
    }

    private static RegularTimePeriod[] createTimeDomain(AxisConfig domain, ChartDataProvider dataProvider) {
        final int index = domain.getIndex();
        final int rowsCount = dataProvider.getRowCount();
        final RegularTimePeriod[] res = new RegularTimePeriod[rowsCount];
        for (int row = 0; row < rowsCount; row++) {
            res[row] = createPeriodValue(dataProvider.getValueAt(row, index));
        }
        return res;
    }

    private static RegularTimePeriod createPeriodValue(Object value) {
        // SQL Date, Time, Timestamp are here
        if (value instanceof Date) {
            return new Millisecond((Date) value);
        } else if (value instanceof c.Second) {
            final c.Second v = (c.Second) value;
            return new Second(new Date(v.i * 1000L));
        } else if (value instanceof c.Minute) {
            final c.Minute v = (c.Minute) value;
            return new Minute(new Date(v.i * 60 * 1000L));
        } else if (value instanceof c.Month) {
            final c.Month v = (c.Month) value;
            return new Month(new Date(v.i * 12 * 24 * 60 * 1000L));
        } else if (value instanceof c.Timespan) {
            final c.Timespan v = (c.Timespan) value;
            return new Millisecond(new Date(v.j / 1_000_000L));
        }
        throw new IllegalArgumentException("Invalid value type: " + value.getClass());
    }
}
