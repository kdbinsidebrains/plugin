package org.kdb.inside.brains.view.console.chart.line;

import com.intellij.ui.JBColor;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.*;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardCrosshairLabelGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.kdb.inside.brains.view.console.chart.BaseChartPanel;
import org.kdb.inside.brains.view.console.chart.ChartDataProvider;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class LineChartPanel extends BaseChartPanel {
    public LineChartPanel(ChartConfig config, ChartDataProvider dataProvider) {
        super(createChart(config, dataProvider));
        XYCrosshairOverlay.register(this);
    }

    private static JFreeChart createChart(ChartConfig config, ChartDataProvider dataProvider) {
        final ColumnConfig domain = config.getDomainValues();
        final Map<String, List<ColumnConfig>> ranges = config.dataset();

        final XYDataset[] datasets = ColumnConfig.isTemporalType(domain.getType()) ? createTimeDatasets(domain, ranges, dataProvider) : createNumberDatasets(domain, ranges, dataProvider);
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(null, domain.getName(), "", null, true, true, false);

        int i = 0;
        final XYPlot plot = chart.getXYPlot();
        for (Map.Entry<String, List<ColumnConfig>> entry : ranges.entrySet()) {
            final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, false);

            int c = 0;
            final List<ColumnConfig> value = entry.getValue();
            for (ColumnConfig column : value) {
                renderer.setSeriesPaint(c, column.getColor());
                renderer.setSeriesStroke(c, new BasicStroke(2.0f));
                renderer.setSeriesLinesVisible(c, column.getLineStyle().isLineVisible());
                renderer.setSeriesShapesVisible(c, column.getLineStyle().isShapeVisible());
                c++;
            }

            plot.setDataset(i, datasets[i]);
            plot.setRenderer(i, renderer);
            plot.setRangeAxis(i, applyAxisColorSchema(new NumberAxis(entry.getKey())));
            plot.setRangeAxisLocation(i, i % 2 == 0 ? AxisLocation.BOTTOM_OR_LEFT : AxisLocation.BOTTOM_OR_RIGHT);
            plot.mapDatasetToRangeAxis(i, i);

            i++;
        }

        applyColorSchema(chart);
        return chart;
    }

    private static XYDataset[] createNumberDatasets(ColumnConfig domainCfg, Map<String, List<ColumnConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final Number[] domain = createNumberDomain(domainCfg, dataProvider);
        final XYDataset[] res = new XYDataset[datasets.size()];
        for (Map.Entry<String, List<ColumnConfig>> entry : datasets.entrySet()) {
            final XYSeriesCollection series = new XYSeriesCollection();
            for (ColumnConfig column : entry.getValue()) {
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

    private static XYDataset[] createTimeDatasets(ColumnConfig domainCfg, Map<String, List<ColumnConfig>> datasets, ChartDataProvider dataProvider) {
        int i = 0;
        final RegularTimePeriod[] domain = createTimeDomain(domainCfg, dataProvider);
        final XYDataset[] res = new XYDataset[datasets.size()];
        for (Map.Entry<String, List<ColumnConfig>> entry : datasets.entrySet()) {
            final TimeSeriesCollection series = new TimeSeriesCollection();
            for (ColumnConfig column : entry.getValue()) {
                final TimeSeries s = new TimeSeries(column.getName());
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], (Double) dataProvider.getValueAt(j, column.getIndex()));
                }
                series.addSeries(s);
            }
            res[i++] = series;
        }
        return res;
    }
/*
    private static void createDatasets(ColumnConfig domainCfg, Map<String, List<ColumnConfig>> datasets, ChartDataProvider dataProvider) {
        final RegularTimePeriod[] domain = createTimeDomain(domainCfg, dataProvider);

        int i = 0;
        for (Map.Entry<String, List<ColumnConfig>> entry : datasets.entrySet()) {
            final String axisName = entry.getKey();
            final TimeSeriesCollection series = new TimeSeriesCollection();
            final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, false);

            int c = 0;
            for (ColumnConfig column : entry.getValue()) {
                renderer.setSeriesPaint(c, column.getColor());
                renderer.setSeriesStroke(c, new BasicStroke(2.0f));
                renderer.setSeriesLinesVisible(c, column.getLineStyle().isLineVisible());
                renderer.setSeriesShapesVisible(c, column.getLineStyle().isShapeVisible());

                final TimeSeries s = new TimeSeries(column.getName());
                for (int j = 0; j < domain.length; j++) {
                    s.addOrUpdate(domain[j], (Double) dataProvider.getValueAt(j, column.getIndex()));
                }
                series.addSeries(s);
                c++;
            }


            i++;
        }
    }*/
/*

    private static RegularTimePeriod[] createNumberDomain(ColumnConfig domain, ChartDataProvider dataProvider) {

    }
*/

    private static Number[] createNumberDomain(ColumnConfig domain, ChartDataProvider dataProvider) {
        final int index = domain.getIndex();
        final int rowsCount = dataProvider.getRowCount();
        final Number[] res = new Number[rowsCount];
        for (int row = 0; row < rowsCount; row++) {
            res[row] = (Number) dataProvider.getValueAt(row, index);
        }
        return res;
    }

    private static RegularTimePeriod[] createTimeDomain(ColumnConfig domain, ChartDataProvider dataProvider) {
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

    private static void applyColorSchema(JFreeChart chart) {
        chart.setBorderPaint(JBColor.foreground());
        chart.setBackgroundPaint(JBColor.background());

        final LegendTitle legend = chart.getLegend();
        legend.setFrame(BlockBorder.NONE);
        legend.setItemPaint(JBColor.foreground());
        legend.setBackgroundPaint(JBColor.background());

        final XYPlot plot = chart.getXYPlot();
        plot.setRangePannable(true);
        plot.setDomainPannable(true);
        plot.setBackgroundPaint(JBColor.background());

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4)));

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4)));

        final int domainAxisCount = plot.getDomainAxisCount();
        for (int i = 0; i < domainAxisCount; i++) {
            applyAxisColorSchema(plot.getDomainAxis(i));
        }
    }

    private static ValueAxis applyAxisColorSchema(ValueAxis axis) {
        axis.setLabelPaint(JBColor.foreground());
        axis.setAxisLinePaint(JBColor.foreground());
        axis.setTickLabelPaint(JBColor.foreground());

        if (axis instanceof NumberAxis) {
            ((NumberAxis) axis).setAutoRangeIncludesZero(false);
        }
        return axis;
    }

    static class XYCrosshairOverlay implements ChartMouseListener {
        private final ChartPanel panel;

        private final Crosshair xCrosshair;
        private final Crosshair yCrosshair;

        public XYCrosshairOverlay(ChartPanel panel) {
            this.panel = panel;

            xCrosshair = createCrosshair(false);
            yCrosshair = createCrosshair(true);

            final CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
            crosshairOverlay.addDomainCrosshair(this.xCrosshair);
            crosshairOverlay.addRangeCrosshair(this.yCrosshair);

            panel.addOverlay(crosshairOverlay);
            panel.addChartMouseListener(this);
        }

        @Override
        public void chartMouseClicked(ChartMouseEvent event) {
        }

        @Override
        public void chartMouseMoved(ChartMouseEvent event) {
            Rectangle2D dataArea = panel.getScreenDataArea();
            JFreeChart chart = event.getChart();
            XYPlot plot = (XYPlot) chart.getPlot();
            ValueAxis xAxis = plot.getDomainAxis();
            ValueAxis yAxis = plot.getRangeAxis();
            double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
            if (!xAxis.getRange().contains(x)) {
                x = Double.NaN;
            }
            double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);
            if (!yAxis.getRange().contains(y)) {
                y = Double.NaN;
            }
            //            double y = DatasetUtils.findYValue(plot.getDataset(), 0, x);
            xCrosshair.setValue(x);
            yCrosshair.setValue(y);
        }

        @NotNull
        private Crosshair createCrosshair(boolean vertical) {
            final Crosshair crosshair = new Crosshair(Double.NaN, new Color(0xa4a4a5), new BasicStroke(0.5F));
            crosshair.setLabelVisible(true);
            crosshair.setLabelAnchor(vertical ? RectangleAnchor.LEFT : RectangleAnchor.BOTTOM);
            crosshair.setLabelPaint(new Color(0x595959));
            crosshair.setLabelOutlinePaint(new Color(0xe0e0e0));
            crosshair.setLabelBackgroundPaint(new Color(0xc4c4c4));
            final ValueAxis domainAxis = ((XYPlot) panel.getChart().getPlot()).getDomainAxis();
            if (!vertical && domainAxis instanceof DateAxis) {
                final DateAxis dateAxis = (DateAxis) domainAxis;
                crosshair.setLabelGenerator(g -> dateAxis.getTickUnit().valueToString(g.getValue()));
            } else {
                crosshair.setLabelGenerator(new StandardCrosshairLabelGenerator("  {0}  ", NumberFormat.getNumberInstance()));
            }

            return crosshair;
        }

        static void register(ChartPanel panel) {
            new XYCrosshairOverlay(panel);
        }
    }
}
