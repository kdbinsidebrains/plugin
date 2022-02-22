package org.kdb.inside.brains.view.chart;

import com.intellij.ui.JBColor;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;

import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;

public class BaseChartPanel extends ChartPanel {
    private static final JBColor COLOR_GRID = new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4));

    public BaseChartPanel(JFreeChart chart) {
        super(chart, false, false, false, false, false);

        // We don't need it. Have own
        setPopupMenu(null);

        setFocusable(true);
        setMouseZoomable(true);
        setMouseWheelEnabled(true);

        fixPanMask();
        fixChartColors(chart);
    }

    private void fixChartColors(JFreeChart chart) {
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

    @SuppressWarnings("deprecation")
    private void fixPanMask() {
        try {
            final Field panMask = ChartPanel.class.getDeclaredField("panMask");
            panMask.setAccessible(true);
            panMask.set(this, InputEvent.BUTTON1_MASK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
}
