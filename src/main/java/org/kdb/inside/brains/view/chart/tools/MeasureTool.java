package org.kdb.inside.brains.view.chart.tools;

import com.intellij.ui.JBColor;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.panel.AbstractOverlay;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.kdb.inside.brains.view.chart.BaseChartPanel;
import org.kdb.inside.brains.view.chart.ChartColors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MeasureTool extends AbstractOverlay implements Overlay, ChartMouseListener {
    private boolean enabled;
    private MeasureArea activeArea;

    private final ChartPanel myPanel;
    private final List<MeasureArea> pinnedAreas = new ArrayList<>();

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");
    private static final KeyStroke KEYSTROKE_ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    static {
        NUMBER_FORMAT.setPositivePrefix("+");
        NUMBER_FORMAT.setNegativePrefix("-");
    }

    public MeasureTool(BaseChartPanel panel) {
        myPanel = panel;

        myPanel.addOverlay(this);
        myPanel.addChartMouseListener(this);
        myPanel.registerKeyboardAction(e -> cancel(), KEYSTROKE_ESC, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public void setChart(JFreeChart chart) {
        activeArea = null;
        pinnedAreas.clear();

        fireOverlayChanged();
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (!enabled) {
            return;
        }

        if (activeArea == null) {
            activeArea = new MeasureArea(calculateValuesPoint(event));
        } else {
            activeArea.finish = calculateValuesPoint(event);
            pinnedAreas.add(activeArea);
            activeArea = null;
        }
        fireOverlayChanged();
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
        if (!enabled) {
            return;
        }
        if (activeArea != null) {
            activeArea.finish = calculateValuesPoint(event);
        }
    }

    private Point2D calculateValuesPoint(ChartMouseEvent event) {
        final JFreeChart chart = event.getChart();

        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis xAxis = plot.getDomainAxis();
        final ValueAxis yAxis = plot.getRangeAxis();
        final Rectangle2D dataArea = myPanel.getScreenDataArea();

        final double x = xAxis.java2DToValue(event.getTrigger().getX(), dataArea, RectangleEdge.BOTTOM);
        final double y = yAxis.java2DToValue(event.getTrigger().getY(), dataArea, RectangleEdge.LEFT);

        return new Point2D.Double(x, y);
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (activeArea == null && pinnedAreas.isEmpty()) {
            return;
        }

        final Shape savedClip = g2.getClip();

        final JFreeChart chart = chartPanel.getChart();
        final Rectangle2D screenArea = chartPanel.getScreenDataArea();
        final XYPlot plot = (XYPlot) chart.getPlot();

        g2.clip(screenArea);

        for (MeasureArea pinnedArea : pinnedAreas) {
            pinnedArea.draw(g2, plot, screenArea);
        }

        if (activeArea != null && activeArea.finish != null) {
            activeArea.draw(g2, plot, screenArea);
        }

        g2.setClip(savedClip);
    }

    public void cancel() {
        if (activeArea != null) {
            activeArea = null;
            fireOverlayChanged();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (!enabled) {
            cancel();
        }
        fireOverlayChanged();
    }

    private static class MeasureArea {
        private final Point2D start;
        private Point2D finish;

        private final Rectangle2D area = new Rectangle2D.Double();

        public MeasureArea(Point2D start) {
            this.start = start;
        }

        public void draw(Graphics2D g2, XYPlot plot, Rectangle2D screenArea) {
            updateDrawingArea(plot, screenArea);

            final double sy = start.getY();
            final double fy = finish.getY();
            final double diffYV = fy - sy;
            final double diffYP = diffYV * 100d / Math.max(sy, fy);
            final Color foreground = sy < fy ? ChartColors.POSITIVE : ChartColors.NEGATIVE;
            final Color background = sy < fy ? ChartColors.POSITIVE_40 : ChartColors.NEGATIVE_40;
            final Color border = background.darker();

            g2.setStroke(new BasicStroke(2));
            g2.setPaint(background);
            g2.fill(area);
            g2.setPaint(border);
            g2.draw(area);

            drawRangeLabel(g2, diffYV, diffYP, foreground);
            drawDomainLabel(g2, plot, screenArea);
        }

        private void drawDomainLabel(Graphics2D g2, XYPlot plot, Rectangle2D screenArea) {
            final double diff = Math.abs(finish.getX() - start.getX());

            String label;
            final ValueAxis domain = plot.getDomainAxis();
            if (domain instanceof DateAxis) {
                label = DurationFormatUtils.formatDurationHMS((long) diff);
            } else {
                label = NUMBER_FORMAT.format(diff);
            }

            g2.setPaint(JBColor.foreground());
            final float x = (float) (area.getX() + area.getWidth() / 2);
            TextUtils.drawAlignedString(label, g2, x, (float) (area.getY() + area.getHeight()) + 15, TextAnchor.BOTTOM_CENTER);
        }

        private void drawRangeLabel(Graphics2D g2, double diffYV, double diffYP, Color foreground) {
            g2.setPaint(foreground);
            final float y = (float) (area.getY() + area.getHeight() / 2);
            TextUtils.drawAlignedString(NUMBER_FORMAT.format(diffYV), g2, (float) (area.getX() + area.getWidth()) + 5, y, TextAnchor.CENTER_LEFT);
            g2.setPaint(JBColor.foreground());
            TextUtils.drawAlignedString(NUMBER_FORMAT.format(diffYP) + "%", g2, (float) (area.getX() + area.getWidth()) + 5, y + 15, TextAnchor.CENTER_LEFT);
        }

        private void updateDrawingArea(XYPlot plot, Rectangle2D screenArea) {
            final ValueAxis domain = plot.getDomainAxis();
            final ValueAxis range = plot.getRangeAxis();
            final RectangleEdge domainEdge = plot.getDomainAxisEdge();
            final RectangleEdge rangeEdge = plot.getRangeAxisEdge();
            final double x1 = domain.valueToJava2D(start.getX(), screenArea, domainEdge);
            final double y1 = range.valueToJava2D(start.getY(), screenArea, rangeEdge);
            final double x2 = domain.valueToJava2D(finish.getX(), screenArea, domainEdge);
            final double y2 = range.valueToJava2D(finish.getY(), screenArea, rangeEdge);
            if (y1 < y2) {
                if (x1 < x2) {
                    area.setRect(x1, y1, x2 - x1, y2 - y1);
                } else {
                    area.setRect(x2, y1, x1 - x2, y2 - y1);
                }
            } else {
                if (x1 < x2) {
                    area.setRect(x1, y2, x2 - x1, y1 - y2);
                } else {
                    area.setRect(x2, y2, x1 - x2, y1 - y2);
                }
            }
        }
    }
}