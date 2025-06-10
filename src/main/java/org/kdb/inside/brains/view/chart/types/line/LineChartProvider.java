package org.kdb.inside.brains.view.chart.types.line;

import kx.KxConnection;
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
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.*;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartColors;
import org.kdb.inside.brains.view.chart.ChartColumn;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ChartViewProvider;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LineChartProvider extends ChartViewProvider<LineConfigPanel, LineChartConfig> {
    private static final DateTickUnit DATE_TICK_UNIT = new DateTickUnit(DateTickUnitType.DAY, 1);

    public LineChartProvider(ChartDataProvider dataProvider) {
        super("Line Chart", ChartType.LINE, dataProvider);
    }

    @Override
    public LineChartConfig createChartConfig() {
        return configPanel.createChartConfig();
    }

    @Override
    public void updateChartConfig(LineChartConfig config) {
        configPanel.updateChartConfig(config);
    }

    @Override
    protected JFreeChart createJFreeChart(LineChartConfig config) {
        return config == null || config.isInvalid() ? null : createChart(config, dataProvider);
    }

    @Override
    protected LineConfigPanel createConfigPanel(ChartDataProvider provider) {
        return new LineConfigPanel(provider, this::processConfigChanged);
    }

    private JFreeChart createChart(LineChartConfig config, ChartDataProvider dataProvider) {
        final JFreeChart chart;
        final XYDataset[] datasets;

        final ChartColumn domain = config.domain();
        final Map<SeriesDefinition, List<SingleRange>> dataset = config.dataset(dataProvider);
        if (ChartColumn.isTemporal(domain.type())) {
            datasets = createTimeDatasets(domain, dataset, dataProvider);
            chart = ChartFactory.createTimeSeriesChart(null, domain.name(), "", null, true, true, false);

            final XYPlot xyPlot = chart.getXYPlot();
            if (domain.type() == KdbType.DATE) {
                final DateAxis newAxis = createFixedDateAxis((DateAxis) xyPlot.getDomainAxis());
                xyPlot.setDomainAxis(newAxis);
            }

            if (xyPlot.getDomainAxis() instanceof DateAxis dx) {
                dx.setTimeZone(KxConnection.UTC_TIMEZONE);
            }
        } else {
            datasets = createNumberDatasets(domain, dataset, dataProvider);
            chart = ChartFactory.createXYLineChart(null, domain.name(), "", null, PlotOrientation.VERTICAL, true, false, false);
        }

        int i = 0;
        int colorIndex = 0;
        final XYPlot plot = chart.getXYPlot();
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.FORWARD);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        for (Map.Entry<SeriesDefinition, List<SingleRange>> entry : dataset.entrySet()) {
            final SeriesDefinition series = entry.getKey();

            final ValueAxis axis = new NumberAxis(series.name());
            axis.setLowerMargin(series.lowerMargin() / 100d);
            axis.setUpperMargin(series.upperMargin() / 100d);

            int j = 0;
            final XYItemRenderer renderer = createRenderer(config, series);
            for (SingleRange ignored : entry.getValue()) {
                renderer.setSeriesPaint(j, ChartColors.getDefaultColor(colorIndex++));
                renderer.setSeriesStroke(j, new BasicStroke(2.f));
                j++;
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
    private DateAxis createFixedDateAxis(DateAxis axis) {
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
    private XYItemRenderer createRenderer(LineChartConfig config, SeriesDefinition series) {
        final SeriesStyle type = series.style();
        if (type == null) {
            throw new UnsupportedOperationException("Series style is not defined");
        }
        return type.createRenderer(config);
    }

    private <P, I> void iterateSingleRanges(SingleRange range, CachedDataProvider dataProvider, P[] periods, Function<P, I> itemGet, BiConsumer<P, Number> itemAddOrUpdate, Function<I, Number> itemValue) {
        final Operation operation = range.values().operation();
        final Number[] numbers = dataProvider.getValues(range.values().column(), ChartDataProvider::getNumbers);

        final Optional<boolean[]> filter = range.expansion().stream()
                .map(k -> {
                    final String value = k.value();
                    final String[] symbols = dataProvider.getValues(k.column(), ChartDataProvider::getSymbols);
                    final boolean[] res = new boolean[symbols.length];
                    for (int i = 0; i < symbols.length; i++) {
                        res[i] = value.equals(symbols[i]);
                    }
                    return res;
                })
                .reduce((a, b) -> {
                    for (int i = 0; i < a.length; i++) {
                        a[i] = a[i] && b[i];
                    }
                    return a;
                });

        int j = 0;
        for (int k = 0; k < periods.length; k++) {
            if (filter.isPresent() && !filter.get()[k]) {
                continue;
            }

            final Number v = numbers[k];
            final P period = periods[k];

            final I item = itemGet.apply(period);
            final Number value = item == null ? operation.initialValue(v) : operation.calc(itemValue.apply(item), v, j);
            itemAddOrUpdate.accept(period, value);
            j++;
        }
    }

    private IntervalXYDataset[] createNumberDatasets(ChartColumn domain, Map<SeriesDefinition, List<SingleRange>> dataset, ChartDataProvider dataProvider) {
        int i = 0;
        final Number[] periods = dataProvider.getNumbers(domain);
        final CachedDataProvider provider = new CachedDataProvider(dataProvider);

        final IntervalXYDataset[] res = new IntervalXYDataset[dataset.size()];
        for (Map.Entry<SeriesDefinition, List<SingleRange>> entry : dataset.entrySet()) {
            final XYSeriesCollection series = new XYSeriesCollection();
            for (SingleRange range : entry.getValue()) {
                final XYSeries s = new XYSeries(range.label(), true, false);
                series.addSeries(s);

                iterateSingleRanges(range, provider, periods, p -> {
                            final int i1 = s.indexOf(p);
                            return i1 < 0 ? null : s.getDataItem(i1);
                        },
                        s::addOrUpdate, XYDataItem::getX
                );
            }
            res[i++] = series;
        }
        return res;
    }

    private IntervalXYDataset[] createTimeDatasets(ChartColumn domain, Map<SeriesDefinition, List<SingleRange>> dataset, ChartDataProvider dataProvider) {
        int i = 0;
        final RegularTimePeriod[] periods = dataProvider.getPeriods(domain);
        final IntervalXYDataset[] res = new IntervalXYDataset[dataset.size()];
        final CachedDataProvider provider = new CachedDataProvider(dataProvider);

        for (Map.Entry<SeriesDefinition, List<SingleRange>> entry : dataset.entrySet()) {
            final TimeSeriesCollection series = new TimeSeriesCollection(KxConnection.UTC_TIMEZONE);
            for (SingleRange range : entry.getValue()) {
                final TimeSeries s = new TimeSeries(range.label());
                series.addSeries(s);

                iterateSingleRanges(range, provider, periods,
                        s::getDataItem, s::addOrUpdate, TimeSeriesDataItem::getValue
                );
            }
            res[i++] = series;
        }
        return res;
    }

    private static class CachedDataProvider {
        private final ChartDataProvider dataProvider;
        private final Map<ChartColumn, Object> cache = new HashMap<>();

        public CachedDataProvider(ChartDataProvider dataProvider) {
            this.dataProvider = dataProvider;
        }

        @SuppressWarnings("unchecked")
        <T> T getValues(ChartColumn column, BiFunction<ChartDataProvider, ChartColumn, T> function) {
            return (T) cache.computeIfAbsent(column, c -> function.apply(dataProvider, c));
        }
    }
}