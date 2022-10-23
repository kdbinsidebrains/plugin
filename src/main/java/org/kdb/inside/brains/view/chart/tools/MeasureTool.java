package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.ui.JBColor;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
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
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.view.chart.BaseChartPanel;
import org.kdb.inside.brains.view.chart.ChartColors;
import org.kdb.inside.brains.view.chart.ChartTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class MeasureTool extends AbstractOverlay implements ChartTool, Overlay, ChartMouseListener {
    private boolean enabled;

    private MeasureArea activeArea;
    private static final KeyStroke KEYSTROKE_DEL = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

    private final BaseChartPanel myPanel;
    private final List<MeasureArea> pinnedAreas = new ArrayList<>();

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    private static final KeyStroke KEYSTROKE_ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private static final Stroke BASIC = new BasicStroke(2);
    private static final Stroke DASHED = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
    private MeasureArea highlighted;

    static {
        NUMBER_FORMAT.setPositivePrefix("+");
        NUMBER_FORMAT.setNegativePrefix("-");
    }

    public MeasureTool(BaseChartPanel panel) {
        myPanel = panel;

        myPanel.addOverlay(this);
        myPanel.addChartMouseListener(this);
        myPanel.registerKeyboardAction(e -> cancel(), KEYSTROKE_ESC, JComponent.WHEN_IN_FOCUSED_WINDOW);
        myPanel.registerKeyboardAction(e -> remove(), KEYSTROKE_DEL, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    @Override
    public void setChart(JFreeChart chart) {
/*
        final String chartSnapshot = getChartSnapshot(chart);
        if (!chartSnapshot.equals(currentChartSnapshot)) {
            activeArea = null;
            pinnedAreas.clear();
            fireOverlayChanged();
        }
*/
    }

    @Override
    public ActionGroup getPopupActions() {
        if (!enabled) {
            return ActionGroup.EMPTY_GROUP;
        }

        return new DefaultActionGroup("Measure",
                List.of(
                        new DumbAwareAction("Remove Measure") {
                            @Override
                            public void update(@NotNull AnActionEvent e) {
                                e.getPresentation().setEnabled(highlighted != null);
                            }

                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e) {
                                remove();
                            }
                        },

                        new DumbAwareAction("Clear Measures") {
                            @Override
                            public void update(@NotNull AnActionEvent e) {
                                e.getPresentation().setEnabled(!pinnedAreas.isEmpty());
                            }

                            @Override
                            public void actionPerformed(@NotNull AnActionEvent e) {
                                clear();
                            }
                        }
                )
        );
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (!enabled) {
            return;
        }

        final MouseEvent trigger = event.getTrigger();
        if (trigger.getClickCount() != 1 || !SwingUtilities.isLeftMouseButton(trigger)) {
            if (activeArea != null) {
                activeArea = null;
                fireOverlayChanged();
            }
            return;
        }

        if (activeArea == null) {
            activeArea = new MeasureArea(myPanel.calculateValuesPoint(event));
        } else {
            activeArea.finish = myPanel.calculateValuesPoint(event);
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

        final Point2D point = myPanel.calculateValuesPoint(event);

        highlighted = findPinnedAreaByPoint(point);

        if (activeArea != null) {
            activeArea.finish = point;
        }
    }

    private MeasureArea findPinnedAreaByPoint(Point2D point) {
        final ListIterator<MeasureArea> iterator = pinnedAreas.listIterator(pinnedAreas.size());
        while (iterator.hasPrevious()) {
            final MeasureArea area = iterator.previous();
            if (area.contains(point)) {
                return area;
            }
        }
        return null;
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

        if (highlighted != null) {
            highlighted.draw(g2, plot, screenArea, true);
        }

        g2.setClip(savedClip);
    }

    public void cancel() {
        if (activeArea != null) {
            activeArea = null;
            fireOverlayChanged();
        }
    }

    public void remove() {
        if (highlighted != null) {
            pinnedAreas.remove(highlighted);
            highlighted = null;
            fireOverlayChanged();
        }
    }

    public void clear() {
        activeArea = null;
        highlighted = null;
        pinnedAreas.clear();
        fireOverlayChanged();
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

    /**
     * Can be used to identify has the chart dataset been changed to reset drawings.
     */
    private String getChartSnapshot(JFreeChart chart) {
        if (chart == null) {
            return "";
        }

        final XYPlot plot = chart.getXYPlot();

        final StringBuilder b = new StringBuilder(plot.getDomainAxis().getLabel());
        final int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            b.append('-').append(plot.getRangeAxis(i).getLabel());
        }

        final int datasetCount = plot.getDatasetCount();
        for (int i = 0; i < datasetCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            final int seriesCount = dataset.getSeriesCount();
            for (int j = 0; j < seriesCount; j++) {
                b.append('-').append(dataset.getSeriesKey(j));
            }
        }
        return b.toString();
    }

    private static class MeasureArea {
        private final Point2D start;
        private Point2D finish;

        private final Rectangle2D area = new Rectangle2D.Double();

        public MeasureArea(Point2D start) {
            this.start = start;
        }

        public void draw(Graphics2D g2, XYPlot plot, Rectangle2D screenArea) {
            draw(g2, plot, screenArea, false);
        }

        public void draw(Graphics2D g2, XYPlot plot, Rectangle2D screenArea, boolean highlight) {
            updateDrawingArea(plot, screenArea);

            final double sy = start.getY();
            final double fy = finish.getY();
            final double diffYV = fy - sy;
            final double diffYP = diffYV * 100d / Math.max(sy, fy);
            final Color foreground = sy < fy ? ChartColors.POSITIVE : ChartColors.NEGATIVE;
            final Color background = sy < fy ? ChartColors.POSITIVE_40 : ChartColors.NEGATIVE_40;
            final Color border = background.darker();

            g2.setStroke(BASIC);
            g2.setPaint(background);
            g2.fill(area);

            g2.setPaint(border);
            if (highlight) {
                g2.setStroke(DASHED);
            }
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

        public boolean contains(Point2D point) {
            if (outside(point.getX(), start.getX(), finish.getX())) {
                return false;
            }
            if (outside(point.getY(), start.getY(), finish.getY())) {
                return false;
            }
            return true;
        }

        private boolean outside(double a, double a1, double a2) {
            if (a1 < a2) {
                return a < a1 || a > a2;
            }
            return a < a2 || a > a1;
        }
    }
}