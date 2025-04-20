package org.kdb.inside.brains.view.chart.tools;

import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.geom.Rectangle2D;

public abstract class AbstractChartTool extends AbstractOverlay implements ChartTool {
    @NotNull
    @Identifier
    private final String id;

    private final String text;
    private final String description;
    private final Icon icon;

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

    protected double calculateDomainPoint(ChartMouseEvent event, Rectangle2D dataArea, boolean nearestPoint) {
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

    protected double calculateRangePoint(ChartMouseEvent event, Rectangle2D dataArea, int rangeIndex) {
        final JFreeChart chart = event.getChart();
        if (!(chart.getPlot() instanceof XYPlot plot)) {
            return Double.NaN;
        }
        final ValueAxis yAxis = plot.getRangeAxis(rangeIndex);
        return yAxis.java2DToValue(event.getTrigger().getY(), dataArea, plot.getRangeAxisEdge());
    }

    //    public Point2D calculateFirstValuePoint(ChartPanel panel, ChartMouseEvent event) {
//        final double x = calculateDomainPoint(panel, event);
//        final double y = calculateValuePoint(event, x, 0, 0);
//        return new Point2D.Double(x, y);
//    }
//

    protected double calculateValuePoint(ChartMouseEvent event, double domain, int datasetIndex, int seriesIndex) {
        final JFreeChart chart = event.getChart();
        if (!(chart.getPlot() instanceof XYPlot plot)) {
            return Double.NaN;
        }
        final XYDataset dataset = plot.getDataset(datasetIndex);
        return DatasetUtils.findYValue(dataset, seriesIndex, domain);
    }
}