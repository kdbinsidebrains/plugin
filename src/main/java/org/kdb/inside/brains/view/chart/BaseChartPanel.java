package org.kdb.inside.brains.view.chart;

import com.intellij.ui.JBColor;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;

public class BaseChartPanel extends ChartPanel {
    private boolean defaultCursor = false;

    private static final JBColor COLOR_GRID = new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4));

    public BaseChartPanel() {
        this(null);
    }

    public BaseChartPanel(JFreeChart chart) {
        super(chart, false, false, false, false, false);

        // We don't need it. Have own
        setPopupMenu(null);

        setFocusable(true);
        setMouseWheelEnabled(true);

        fixPanMask();
    }

    protected static void applyColorSchema(XYPlot plot) {
        plot.setRangePannable(true);
        plot.setDomainPannable(true);
        plot.setBackgroundPaint(JBColor.background());
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(COLOR_GRID);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(COLOR_GRID);

        final int domainAxisCount = plot.getDomainAxisCount();
        for (int i = 0; i < domainAxisCount; i++) {
            applyAxisColorSchema(plot.getDomainAxis(i));
        }

        final int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            applyAxisColorSchema(plot.getRangeAxis(i));
        }
    }

    @Override
    public void setChart(JFreeChart chart) {
        super.setChart(chart);

        if (chart != null) {
            setMouseZoomable(true);
            applyCharSchema(chart);
        }
    }

    @Override
    public void setCursor(Cursor cursor) {
        if (defaultCursor) {
            if (cursor != Cursor.getDefaultCursor()) {
                super.setCursor(Cursor.getDefaultCursor());
            }
        } else {
            super.setCursor(cursor);
        }
    }

    public void setDefaultCursor(boolean defaultCursor) {
        this.defaultCursor = defaultCursor;
    }

    @SuppressWarnings("deprecation")
    private void fixPanMask() {
        try {
            final Field panMask = ChartPanel.class.getDeclaredField("panMask");
            panMask.setAccessible(true);
            panMask.set(this, InputEvent.BUTTON1_MASK);
        } catch (Exception ignore) {
            // not required
        }
    }

    private void applyCharSchema(JFreeChart chart) {
        chart.setBorderPaint(JBColor.foreground());
        chart.setBackgroundPaint(JBColor.background());

        final LegendTitle legend = chart.getLegend();
        if (legend != null) {
            legend.setFrame(BlockBorder.NONE);
            legend.setItemPaint(JBColor.foreground());
            legend.setBackgroundPaint(JBColor.background());
        }

        final Plot plot = chart.getPlot();
        if (plot instanceof XYPlot) {
            applyColorSchema((XYPlot) plot);
        }
    }

    protected static ValueAxis applyAxisColorSchema(ValueAxis axis) {
        axis.setLabelPaint(JBColor.foreground());
        axis.setAxisLinePaint(JBColor.foreground());
        axis.setTickLabelPaint(JBColor.foreground());

        if (axis instanceof NumberAxis) {
            ((NumberAxis) axis).setAutoRangeIncludesZero(false);
        }
        return axis;
    }

    public Point2D calculateValuesPoint(ChartMouseEvent event) {
        final JFreeChart chart = event.getChart();

        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis xAxis = plot.getDomainAxis();
        final ValueAxis yAxis = plot.getRangeAxis();
        final Rectangle2D dataArea = getScreenDataArea();

        final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

        return new Point2D.Double(x, y);
    }
}
