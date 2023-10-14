package org.kdb.inside.brains.view.chart.types.line;

import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ChartViewProvider;
import org.kdb.inside.brains.view.chart.ColumnConfig;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.awt.*;
import java.util.List;
import java.util.Map;

public class LineChartProvider extends ChartViewProvider<LineConfigPanel, LineChartConfig> {
    private static final DateTickUnit DATE_TICK_UNIT = new DateTickUnit(DateTickUnitType.DAY, 1);

    public LineChartProvider(ChartDataProvider dataProvider) {
        super("Line Chart", ChartType.LINE, dataProvider);
    }

    private static JFreeChart createChart(LineChartConfig config, ChartDataProvider dataProvider) {
        final ColumnConfig domain = config.getDomain();
        final Map<SeriesConfig, List<RangeConfig>> ranges = config.dataset();

        final XYDataset[] datasets;
        final JFreeChart chart;
        if (RangeConfig.isTemporal(domain.getType())) {
            datasets = createTimeDatasets(domain, ranges, dataProvider);
            chart = ChartFactory.createTimeSeriesChart(null, domain.getName(), "", null, true, true, false);

            if (domain.getType() == KdbType.DATE) {
                final XYPlot xyPlot = chart.getXYPlot();
                final DateAxis newAxis = createFixedDateAxis((DateAxis) xyPlot.getDomainAxis());
                xyPlot.setDomainAxis(newAxis);
            }
        } else {
            datasets = createNumberDatasets(domain, ranges, dataProvider);
            chart = ChartFactory.createXYLineChart(null, domain.getName(), "", null, PlotOrientation.VERTICAL, true, false, false);
        }

        int i = 0;
        final XYPlot plot = chart.getXYPlot();
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : ranges.entrySet()) {
            final SeriesConfig series = entry.getKey();
            final List<RangeConfig> axes = entry.getValue();

            final ValueAxis axis = new NumberAxis(series.getName());
            axis.setLowerMargin(series.getLowerMargin() / 100d);
            axis.setUpperMargin(series.getUpperMargin() / 100d);

            final XYItemRenderer renderer = createRenderer(config, series, axes);
            for (int j = 0; j < axes.size(); j++) {
                final RangeConfig ac = axes.get(j);
                renderer.setSeriesPaint(j, ac.getColor());
                renderer.setSeriesStroke(j, new BasicStroke(ac.getWidth()));
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
    private static DateAxis createFixedDateAxis(DateAxis axis) {
        final DateAxis newAxis = new DateAxis(axis.getLabel(), axis.getTimeZone(), axis.getLocale()) {
            @Override
            public void setTickUnit(DateTickUnit unit, boolean notify, boolean turnOffAutoSelection) {
                final DateTickUnitType unitType = unit.getUnitType();
                if (unitType != DateTickUnitType.DAY && unitType != DateTickUnitType.MONTH && unitType != DateTickUnitType.YEAR) {
                    unit = (DateTickUnit) getStandardTickUnits().getCeilingTickUnit(DATE_TICK_UNIT);
                }
                super.setTickUnit(unit, notify, turnOffAutoSelection);
            }
        };
        newAxis.setLowerMargin(axis.getLowerMargin());
        newAxis.setUpperMargin(axis.getUpperMargin());
        return newAxis;
    }

    @NotNull
    private static XYItemRenderer createRenderer(LineChartConfig config, SeriesConfig series, List<RangeConfig> axes) {
        final SeriesType type = series.getType();
        if (type == null) {
            throw new UnsupportedOperationException("No renderer for type " + series.getType());
        }
        return type.createRenderer(config, axes);
    }

    private static IntervalXYDataset[] createNumberDatasets(ColumnConfig domainCfg, Map<SeriesConfig, List<RangeConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final Number[] domain = dataProvider.getNumbers(domainCfg);
        final XYSeriesCollection[] res = new XYSeriesCollection[datasets.size()];
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : datasets.entrySet()) {
            final XYSeriesCollection series = new XYSeriesCollection();
            for (RangeConfig column : entry.getValue()) {
                final XYSeries s = new XYSeries(column.getName());
                final Number[] numbers = dataProvider.getNumbers(column);
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], numbers[j]);
                }
                series.addSeries(s);
            }
            res[i++] = series;
        }
        return res;
    }

    private static IntervalXYDataset[] createTimeDatasets(ColumnConfig domainCfg, Map<SeriesConfig, List<RangeConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final RegularTimePeriod[] domain = dataProvider.getPeriods(domainCfg);
        final IntervalXYDataset[] res = new IntervalXYDataset[datasets.size()];
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : datasets.entrySet()) {
            final TimeSeriesCollection series = new TimeSeriesCollection();
            for (RangeConfig column : entry.getValue()) {
                final TimeSeries s = new TimeSeries(column.getName());
                final Number[] numbers = dataProvider.getNumbers(column);
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], numbers[j]);
                }
                series.addSeries(s);
            }
            res[i++] = series;
        }
        return res;
    }

    @Override
    protected LineConfigPanel createConfigPanel(ChartDataProvider provider) {
        return new LineConfigPanel(provider, this::processConfigChanged);
    }

    @Override
    public LineChartConfig getChartConfig() {
        return configPanel.getChartConfig();
    }

    @Override
    public void setChartConfig(LineChartConfig config) {
        configPanel.setChartConfig(config);
    }

    @Override
    public JFreeChart getJFreeChart(LineChartConfig config) {
        return config.isInvalid() ? null : createChart(config, dataProvider);
    }
}