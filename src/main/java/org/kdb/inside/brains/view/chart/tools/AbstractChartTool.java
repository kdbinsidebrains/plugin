package org.kdb.inside.brains.view.chart.tools;

import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class AbstractChartTool extends AbstractOverlay implements ChartTool {
    @NotNull
    @Identifier
    private final String id;

    private final String text;
    private final String description;
    private final Icon icon;

    private static final ValueSupplier[] OHLC_SUPPLIERS = new ValueSupplier[]{
            OHLCDataset::getOpenValue,
            OHLCDataset::getHighValue,
            OHLCDataset::getLowValue,
            OHLCDataset::getCloseValue,
            OHLCDataset::getVolumeValue,
    };

    public AbstractChartTool(@NotNull @Identifier String id, String text, String description, Icon icon) {
        this.id = id;
        this.text = text;
        this.description = description;
        this.icon = icon;
    }

    @Override
    @Identifier
    public @NotNull String getId() {
        return id;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Icon getIcon() {
        return icon;
    }

    @Override
    public String getDescription() {
        return description;
    }

    protected static double[] calculateOHLCValues(OHLCDataset ds, int series, double x) {
        final int[] indices = DatasetUtils.findItemIndicesForX(ds, series, x);
        if (indices[0] < 0) {
            return null;
        }

        final double[] values = new double[OHLC_SUPPLIERS.length];
        for (int i = 0; i < OHLC_SUPPLIERS.length; i++) {
            final ValueSupplier supplier = OHLC_SUPPLIERS[i];
            values[i] = getOHLCValue(ds, supplier, series, x, indices[0], indices[1]);
        }
        return values;
    }

    protected static double calculateDomainPoint(ChartMouseEvent event, Rectangle2D dataArea, boolean nearestPoint) {
        final JFreeChart chart = event.getChart();
        if (!(chart.getPlot() instanceof XYPlot plot)) {
            return Double.NaN;
        }

        final ValueAxis axis = plot.getDomainAxis();
        final XYDataset dataset = plot.getDataset();
        final double x = axis.java2DToValue(event.getTrigger().getX(), dataArea, plot.getDomainAxisEdge());
        if (!nearestPoint) {
            return x;
        }

        final int[] ids = DatasetUtils.findItemIndicesForX(dataset, 0, x);
        if (ids[0] >= 0 && ids[1] >= 0) {
            final double x1 = dataset.getX(0, ids[0]).doubleValue();
            final double x2 = dataset.getX(0, ids[1]).doubleValue();
            final double med = (x1 + x2) / 2;
            return x < med ? x1 : x2;
        } else {
            return Double.NaN;
        }
    }

    protected static double calculateRangePoint(ChartMouseEvent event, Rectangle2D dataArea, int rangeIndex) {
        final JFreeChart chart = event.getChart();
        if (!(chart.getPlot() instanceof XYPlot plot)) {
            return Double.NaN;
        }
        final ValueAxis yAxis = plot.getRangeAxis(rangeIndex);
        return yAxis.java2DToValue(event.getTrigger().getY(), dataArea, plot.getRangeAxisEdge());
    }

    public static Point2D calculateFirstValuePoint(ChartMouseEvent event, Rectangle2D dataArea, boolean nearestPoint) {
        final double x = calculateDomainPoint(event, dataArea, nearestPoint);
        final double y = calculateValuePoint(event, x, 0, 0);
        return new Point2D.Double(x, y);
    }

    protected static double calculateValuePoint(ChartMouseEvent event, double domain, int datasetIndex, int seriesIndex) {
        final JFreeChart chart = event.getChart();
        if (!(chart.getPlot() instanceof XYPlot plot)) {
            return Double.NaN;
        }
        final XYDataset dataset = plot.getDataset(datasetIndex);
        return DatasetUtils.findYValue(dataset, seriesIndex, domain);
    }

    // See DatasetUtils#findYValue
    private static double getOHLCValue(OHLCDataset ds, ValueSupplier supplier, int series, double x, int i1, int i2) {
        if (i1 == i2) {
            return supplier.get(ds, series, i1);
        }
        double x0 = ds.getXValue(series, i1);
        double x1 = ds.getXValue(series, i2);
        double y0 = supplier.get(ds, series, i1);
        double y1 = supplier.get(ds, series, i2);
        return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
    }

    @FunctionalInterface
    private interface ValueSupplier {
        double get(OHLCDataset ds, int series, int item);
    }
}