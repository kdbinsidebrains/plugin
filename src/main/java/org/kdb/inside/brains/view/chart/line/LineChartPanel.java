package org.kdb.inside.brains.view.chart.line;

import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kdb.inside.brains.view.chart.BaseChartPanel;
import org.kdb.inside.brains.view.chart.ChartDataProvider;

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
        final Map<SeriesConfig, List<AxisConfig>> ranges = config.dataset();

        final XYDataset[] datasets;
        final JFreeChart chart;
        if (AxisConfig.isTemporalType(domain.getType())) {
            datasets = createTimeDatasets(domain, ranges, dataProvider);
            chart = ChartFactory.createTimeSeriesChart(null, domain.getName(), "", null, true, true, false);
        } else {
            datasets = createNumberDatasets(domain, ranges, dataProvider);
            chart = ChartFactory.createXYLineChart(null, domain.getName(), "", null, PlotOrientation.VERTICAL, true, false, false);
        }

        int i = 0;
        final XYPlot plot = chart.getXYPlot();
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        for (Map.Entry<SeriesConfig, List<AxisConfig>> entry : ranges.entrySet()) {
            final SeriesConfig series = entry.getKey();
            final List<AxisConfig> axes = entry.getValue();

            final ValueAxis axis = applyAxisColorSchema(new NumberAxis(series.getName()));
            axis.setLowerMargin(series.getLowerMargin() / 100d);
            axis.setUpperMargin(series.getUpperMargin() / 100d);

            final XYItemRenderer renderer = createRenderer(config, series, axes);
            for (int j = 0; j < axes.size(); j++) {
                final AxisConfig ac = axes.get(j);
                renderer.setSeriesPaint(j, ac.getColor());
                renderer.setSeriesStroke(j, new BasicStroke(ac.getWidth().floatValue()));
            }

            plot.setDataset(i, datasets[i]);
            plot.setRenderer(i, renderer);
            plot.setRangeAxis(i, axis);
            plot.setRangeAxisLocation(i, i % 2 == 0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
            plot.mapDatasetToRangeAxis(i, i);
            i++;
        }
        return chart;
    }

    @NotNull
    private static XYItemRenderer createRenderer(ChartConfig config, SeriesConfig series, List<AxisConfig> axes) {
        final SeriesType type = series.getType();
        if (type == null) {
            throw new UnsupportedOperationException("No renderer for type " + series.getType());
        }
        return type.createRenderer(config, axes);
    }

    private static IntervalXYDataset[] createNumberDatasets(AxisConfig domainCfg, Map<SeriesConfig, List<AxisConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final Number[] domain = createNumberDomain(domainCfg, dataProvider);
        final XYSeriesCollection[] res = new XYSeriesCollection[datasets.size()];
        for (Map.Entry<SeriesConfig, List<AxisConfig>> entry : datasets.entrySet()) {
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

    private static IntervalXYDataset[] createTimeDatasets(AxisConfig domainCfg, Map<SeriesConfig, List<AxisConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final RegularTimePeriod[] domain = createTimeDomain(domainCfg, dataProvider);
        final IntervalXYDataset[] res = new IntervalXYDataset[datasets.size()];
        for (Map.Entry<SeriesConfig, List<AxisConfig>> entry : datasets.entrySet()) {
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
