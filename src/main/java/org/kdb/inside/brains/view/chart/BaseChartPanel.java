package org.kdb.inside.brains.view.chart;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
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
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.util.function.Supplier;

public class BaseChartPanel extends ChartPanel {
    private SnapType snapType = SnapType.NO;
    private boolean defaultCursor = false;

    private final Supplier<ActionGroup> popupActionsProvider;

    private static final JBColor COLOR_GRID = new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4));

    public BaseChartPanel(Supplier<ActionGroup> popupActionsProvider) {
        this(null, popupActionsProvider);
    }

    public BaseChartPanel(JFreeChart chart, Supplier<ActionGroup> popupActionsProvider) {
        super(chart, false, false, false, false, false);
        this.popupActionsProvider = popupActionsProvider;

        setFocusable(true);
        setMouseWheelEnabled(true);

        // Set mock menu to process displayPopupMenu
        setPopupMenu(new JPopupMenu());

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

    public SnapType getSnapType() {
        return snapType;
    }

    public void setSnapType(SnapType snapType) {
        this.snapType = snapType;
    }

    protected static void applyAxisColorSchema(ValueAxis axis) {
        axis.setLabelPaint(JBColor.foreground());
        axis.setAxisLinePaint(JBColor.foreground());
        axis.setTickLabelPaint(JBColor.foreground());

        if (axis instanceof NumberAxis) {
            ((NumberAxis) axis).setAutoRangeIncludesZero(false);
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

    @Override
    protected void displayPopupMenu(int x, int y) {
        final ActionGroup group = popupActionsProvider.get();
        if (group == null || group.getChildren(null).length == 0) {
            return;
        }

        final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, DataManager.getInstance().getDataContext(this), JBPopupFactory.ActionSelectionAid.MNEMONICS, true);
        final Point p = new Point(x, y);
        SwingUtilities.convertPointToScreen(p, this);
        popup.showInScreenCoordinates(this, p);
    }

    public Point2D calculateValuesPoint(ChartMouseEvent event) {
        final JFreeChart chart = event.getChart();
        final XYPlot plot = (XYPlot) chart.getPlot();
        final XYDataset dataset = plot.getDataset();
        final ValueAxis xAxis = plot.getDomainAxis();
        final ValueAxis yAxis = plot.getRangeAxis();
        final Rectangle2D dataArea = getScreenDataArea();

        double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, plot.getDomainAxisEdge());
        if (snapType == SnapType.VERTEX) {
            final int[] ids = DatasetUtils.findItemIndicesForX(dataset, 0, x);
            if (ids[0] >= 0 && ids[1] >= 0) {
                final double x1 = dataset.getX(0, ids[0]).doubleValue();
                final double x2 = dataset.getX(0, ids[1]).doubleValue();
                final double med = (x1 + x2) / 2;
                x = x < med ? x1 : x2;
            } else {
                x = Double.NaN;
            }
        }

        double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, plot.getRangeAxisEdge());
        if (snapType != SnapType.NO) {
            y = DatasetUtils.findYValue(dataset, 0, x);
        }
        return new Point2D.Double(x, y);
    }
}
