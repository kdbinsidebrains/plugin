package org.kdb.inside.brains.view.console.chart;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardCrosshairLabelGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Field;
import java.text.NumberFormat;

public class BaseChartPanel extends ChartPanel {
    public static final JBColor CROSSHAIR_PAINT = new JBColor(new Color(0xa4a4a5), new Color(0xa4a4a5));
    public static final JBColor CROSSHAIR_LABEL = new JBColor(new Color(0x595959), new Color(0x595959));
    public static final JBColor CROSSHAIR_OUTLINE = new JBColor(new Color(0xe0e0e0), new Color(0xe0e0e0));
    public static final JBColor CROSSHAIR_BACKGROUND = new JBColor(new Color(0xc4c4c4), new Color(0xc4c4c4));

    public BaseChartPanel(JFreeChart chart) {
        super(chart, false, true, true, true, true);
        fixPanMask();

        setMouseZoomable(true);
        setMouseWheelEnabled(true);

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
            initialiXYPlot((XYPlot) plot);
        }
    }

    private void initialiXYPlot(XYPlot plot) {
        applyColorSchema(plot);

        XYCrosshairOverlay.register(this);
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
        plot.setRangeGridlinePaint(new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4)));

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4)));

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

    protected static class XYCrosshairOverlay implements ChartMouseListener {
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
            final Crosshair crosshair = new Crosshair(Double.NaN, CROSSHAIR_PAINT, new BasicStroke(0.5F));
            crosshair.setLabelVisible(true);
            crosshair.setLabelAnchor(vertical ? RectangleAnchor.LEFT : RectangleAnchor.BOTTOM);
            crosshair.setLabelPaint(CROSSHAIR_LABEL);
            crosshair.setLabelOutlinePaint(CROSSHAIR_OUTLINE);
            crosshair.setLabelBackgroundPaint(CROSSHAIR_BACKGROUND);
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
