package org.kdb.inside.brains.view.chart.tools.impl;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.view.chart.ChartColors;
import org.kdb.inside.brains.view.chart.ChartOptions;
import org.kdb.inside.brains.view.chart.ChartViewPanel;
import org.kdb.inside.brains.view.chart.ToolActions;
import org.kdb.inside.brains.view.chart.tools.AbstractChartTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

public class MeasureTool extends AbstractChartTool implements ChartMouseListener {
    private MeasureArea activeArea;
    private MeasureArea highlighted;

    private final ChartViewPanel myPanel;
    private final ChartOptions myOptions;
    private final List<MeasureArea> pinnedAreas = new ArrayList<>();

    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#0.00");

    private static final Stroke BASIC = new BasicStroke(2);
    private static final Stroke DASHED = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);

    public static final String ID = "MEASURE";

    static {
        NUMBER_FORMAT.setPositivePrefix("+");
        NUMBER_FORMAT.setNegativePrefix("-");
    }

    private static final KeyStroke KEYSTROKE_ESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private static final KeyStroke KEYSTROKE_DEL = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

    public MeasureTool(ChartViewPanel panel, ChartOptions options) {
        super(ID, "Measure", "Measuring tool", KdbIcons.Chart.ToolMeasure);

        myPanel = panel;
        myOptions = options;

        myPanel.addOverlay(this);
        myPanel.addChartMouseListener(this);
        myPanel.registerKeyboardAction(e -> cancel(), KEYSTROKE_ESC, JComponent.WHEN_IN_FOCUSED_WINDOW);
        myPanel.registerKeyboardAction(e -> remove(), KEYSTROKE_DEL, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private static String formatPeriod(long startMillis, long endMillis) {
        final Calendar start = Calendar.getInstance(TimeZone.getDefault());
        start.setTime(new Date(startMillis));

        final Calendar end = Calendar.getInstance(TimeZone.getDefault());
        end.setTime(new Date(endMillis));

        int milliseconds = end.get(Calendar.MILLISECOND) - start.get(Calendar.MILLISECOND);
        int seconds = end.get(Calendar.SECOND) - start.get(Calendar.SECOND);
        int minutes = end.get(Calendar.MINUTE) - start.get(Calendar.MINUTE);
        int hours = end.get(Calendar.HOUR_OF_DAY) - start.get(Calendar.HOUR_OF_DAY);
        int days = end.get(Calendar.DAY_OF_MONTH) - start.get(Calendar.DAY_OF_MONTH);
        int months = end.get(Calendar.MONTH) - start.get(Calendar.MONTH);

        int years;
        for (years = end.get(Calendar.YEAR) - start.get(Calendar.YEAR); milliseconds < 0; --seconds) {
            milliseconds += 1000;
        }
        while (seconds < 0) {
            seconds += 60;
            --minutes;
        }
        while (minutes < 0) {
            minutes += 60;
            --hours;
        }
        while (hours < 0) {
            hours += 24;
            --days;
        }
        while (days < 0) {
            days += start.getActualMaximum(Calendar.DATE);
            --months;
            start.add(Calendar.MONTH, 1);
        }
        while (months < 0) {
            months += 12;
            --years;
        }
        return formatTime(years, months, days, hours, minutes, seconds, milliseconds);
    }

    private static StringBuilder prepare(StringBuilder b) {
        if (!b.isEmpty()) {
            b.append(" ");
        }
        return b;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (!isEnabled()) {
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
        if (!isEnabled()) {
            return;
        }

        final Point2D point = myPanel.calculateValuesPoint(event);

        highlighted = findPinnedAreaByPoint(point);

        if (activeArea != null) {
            activeArea.finish = point;
        }
    }

    private static String formatTime(int years, int months, int days, int hours, int minutes, int seconds, int milliseconds) {
        final StringBuilder buffer = new StringBuilder();
        if (years != 0) {
            buffer.append(years).append("Y");
        }
        if (months != 0) {
            prepare(buffer).append(months).append("M");
        }
        if (days != 0) {
            prepare(buffer).append(days).append("D");
        }

        if (!buffer.isEmpty() && hours != 0 && minutes != 0 && seconds != 0 && milliseconds != 0) {
            prepare(buffer).append("T");
        }

        if (hours != 0) {
            prepare(buffer).append(hours).append("H");
        }
        if (minutes != 0) {
            prepare(buffer).append(minutes).append("M");
        }

        if (seconds != 0) {
            prepare(buffer).append(seconds);
            if (milliseconds != 0) {
                buffer.append(".");
                buffer.append(milliseconds);
            }
            buffer.append("S");
        } else if (milliseconds != 0) {
            prepare(buffer).append("0.").append(milliseconds).append("S");
        }
        return buffer.toString();
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
        return myOptions.isEnabled(this);
    }

    public void setEnabled(boolean enabled) {
        myOptions.setEnabled(this, enabled);

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

    @Override
    public void initialize(JFreeChart chart, KdbType domainType) {
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
    public ToolActions getToolActions() {
        if (!isEnabled()) {
            return ToolActions.NO_ACTIONS;
        }

        return new ToolActions("Measure",
                new EdtAction("Remove Measure") {
                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        e.getPresentation().setEnabled(highlighted != null);
                    }

                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        remove();
                    }
                },

                new EdtAction("Clear Measures") {
                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        e.getPresentation().setEnabled(!pinnedAreas.isEmpty());
                    }

                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        clear();
                    }
                }
        );
    }

    private MeasureArea findPinnedAreaByPoint(Point2D point) {
        if (Double.isNaN(point.getX()) || Double.isNaN(point.getY())) {
            return null;
        }

        final ListIterator<MeasureArea> iterator = pinnedAreas.listIterator(pinnedAreas.size());
        while (iterator.hasPrevious()) {
            final MeasureArea area = iterator.previous();
            if (area.contains(point)) {
                return area;
            }
        }
        return null;
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
            drawDomainLabel(g2, plot);
        }

        private void drawDomainLabel(Graphics2D g2, XYPlot plot) {

            String label;
            final ValueAxis domain = plot.getDomainAxis();
            if (domain instanceof DateAxis) {
                final long sp = (long) start.getX();
                final long ep = (long) finish.getX();
                label = (sp > ep) ? formatPeriod(ep, sp) : formatPeriod(sp, ep);
            } else {
                label = NUMBER_FORMAT.format(Math.abs(finish.getX() - start.getX()));
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