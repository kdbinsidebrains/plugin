package org.kdb.inside.brains.view.chart.tools.impl;

import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCrosshairLabelGenerator;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.view.chart.ChartView;
import org.kdb.inside.brains.view.chart.RendererConfig;
import org.kdb.inside.brains.view.chart.SnapType;
import org.kdb.inside.brains.view.chart.tools.AbstractChartTool;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class CrosshairTool extends AbstractChartTool {
    private SnapType snapType;

    private Crosshair chDomain;
    private final List<IndexedCrosshair> chValues = new ArrayList<>();

    private final CrosshairOverlayProxy crosshairProxy = new CrosshairOverlayProxy();

    public static final String ID = "CROSSHAIR";

    private static final BasicStroke STROKE = new BasicStroke(0.5F);
    private static final BasicStroke CROSSHAIR_STROKE = new BasicStroke(1.5F);

    private static final JBColor CROSSHAIR_PAINT = new JBColor(new Color(0xa4a4a5), new Color(0xa4a4a5));
    private static final JBColor CROSSHAIR_LABEL = new JBColor(new Color(0x595959), new Color(0x595959));
    private static final JBColor CROSSHAIR_OUTLINE = new JBColor(new Color(0xe0e0e0), new Color(0xe0e0e0));
    private static final JBColor CROSSHAIR_BACKGROUND = new JBColor(new Color(0xc4c4c4), new Color(0xc4c4c4));

    public CrosshairTool() {
        super(ID, "Crosshair", "Show crosshair lines", KdbIcons.Chart.ToolCrosshair);
    }

    @Override
    public void chartChanged(ChartView view, SnapType snapType) {
        chDomain = null;
        chValues.clear();

        if (view == null) {
            return;
        }

        this.snapType = snapType;

        final XYPlot plot = view.chart().getXYPlot();
        chDomain = createCrosshair(plot, true, plot.getDomainAxisLocation(), 0, 0);
        chValues.addAll(snapType == SnapType.NO ? createAxisCrosshair(plot) : createSnapCrosshair(plot));
        fireOverlayChanged();
    }

    @Override
    public void chartStyleChanged(JFreeChart chart, RendererConfig config, int datasetIndex, int seriesIndex) {
        if (chDomain == null || config.color() == null) {
            return;
        }

        final IndexedCrosshair c = getValuesCrosshair(datasetIndex, seriesIndex);
        if (c != null) {
            c.setLabelOutlinePaint(config.color());
        }

        fireOverlayChanged();
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event, Rectangle2D dataArea) {
        if (chDomain == null) {
            return;
        }

        final double x = calculateDomainPoint(event, dataArea, snapType == SnapType.VERTEX);
        chDomain.setValue(x);

        for (IndexedCrosshair ch : chValues) {
            final double y;
            if (snapType == SnapType.NO) {
                y = calculateRangePoint(event, dataArea, ch.dataset);
            } else {
                y = calculateValuePoint(event, x, ch.dataset, ch.series);
            }
            ch.setValue(y);
        }
        fireOverlayChanged();
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (chDomain == null) {
            return;
        }

        final Shape savedClip = g2.getClip();

        final Rectangle2D dataArea = chartPanel.getScreenDataArea();
        g2.clip(dataArea);

        final JFreeChart chart = chartPanel.getChart();
        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis xAxis = plot.getDomainAxis();
        final RectangleEdge xAxisEdge = plot.getDomainAxisEdge();

        final double x = xAxis.valueToJava2D(chDomain.getValue(), dataArea, xAxisEdge);
        crosshairProxy.drawVerticalCrosshair(g2, dataArea, x, chDomain);

        for (IndexedCrosshair ch : chValues) {
            final ValueAxis yAxis = plot.getRangeAxis(ch.dataset);
            final RectangleEdge yAxisEdge = plot.getRangeAxisEdge(ch.dataset);
            final double y = yAxis.valueToJava2D(ch.getValue(), dataArea, yAxisEdge);
            crosshairProxy.drawHorizontalCrosshair(g2, dataArea, y, ch);
        }

        g2.setClip(savedClip);
    }

    private IndexedCrosshair getValuesCrosshair(int datasetIndex, int seriesIndex) {
        for (IndexedCrosshair c : chValues) {
            if (c.dataset == datasetIndex && c.series == seriesIndex) {
                return c;
            }
        }
        return null;
    }

    private List<IndexedCrosshair> createAxisCrosshair(XYPlot plot) {
        final List<IndexedCrosshair> res = new ArrayList<>();
        final int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            res.add(createCrosshair(plot, false, plot.getRangeAxisLocation(i), i, 0));
        }
        return res;
    }

    private List<IndexedCrosshair> createSnapCrosshair(XYPlot plot) {
        final List<IndexedCrosshair> res = new ArrayList<>();
        final int datasetCount = plot.getDatasetCount();
        for (int set = 0; set < datasetCount; set++) {
            final XYDataset dataset = plot.getDataset(set);
            final AxisLocation rangeAxisLocation = plot.getRangeAxisLocation(set);
            final int seriesCount = dataset.getSeriesCount();
            for (int ser = 0; ser < seriesCount; ser++) {
                final IndexedCrosshair crosshair = createCrosshair(plot, false, rangeAxisLocation, set, ser);
                final Paint paint = plot.getRenderer(set).getSeriesPaint(ser);
                if (paint != null) {
                    crosshair.setLabelOutlinePaint(paint);
                }
                res.add(crosshair);
            }
        }
        return res;
    }

    private IndexedCrosshair createCrosshair(XYPlot plot, boolean domain, AxisLocation location, int datasetIndex, int seriesIndex) {
        final IndexedCrosshair crosshair = new IndexedCrosshair(datasetIndex, seriesIndex);
        crosshair.setPaint(CROSSHAIR_PAINT);
        crosshair.setStroke(STROKE);
        crosshair.setValue(Double.NaN);
        crosshair.setLabelVisible(true);
        crosshair.setLabelPaint(CROSSHAIR_LABEL);
        crosshair.setLabelOutlineVisible(true);
        crosshair.setLabelOutlinePaint(CROSSHAIR_OUTLINE);
        crosshair.setLabelOutlineStroke(CROSSHAIR_STROKE);
        crosshair.setLabelBackgroundPaint(CROSSHAIR_BACKGROUND);

        crosshair.setLabelAnchor(convertLocation(location, domain));
        final ValueAxis domainAxis = plot.getDomainAxis();
        if (domain && domainAxis instanceof DateAxis dateAxis) {
            crosshair.setLabelGenerator(g -> dateAxis.getTickUnit().valueToString(g.getValue()));
        } else {
            crosshair.setLabelGenerator(new StandardCrosshairLabelGenerator("  {0}  ", NumberFormat.getNumberInstance()));
        }
        return crosshair;
    }

    private static RectangleAnchor convertLocation(AxisLocation location, boolean domain) {
        if (location == AxisLocation.BOTTOM_OR_LEFT) {
            return domain ? RectangleAnchor.BOTTOM : RectangleAnchor.LEFT;
        }
        if (location == AxisLocation.TOP_OR_LEFT) {
            return domain ? RectangleAnchor.TOP : RectangleAnchor.LEFT;
        }
        if (location == AxisLocation.BOTTOM_OR_RIGHT) {
            return domain ? RectangleAnchor.BOTTOM : RectangleAnchor.RIGHT;
        }
        if (location == AxisLocation.TOP_OR_RIGHT) {
            return domain ? RectangleAnchor.TOP : RectangleAnchor.RIGHT;
        }
        return null;
    }

    private static class IndexedCrosshair extends Crosshair {
        private final int dataset;
        private final int series;

        public IndexedCrosshair(int dataset, int series) {
            this.dataset = dataset;
            this.series = series;
        }
    }

    /**
     * Simple proxy to get rid of CrosshairOverlay extension
     */
    private static class CrosshairOverlayProxy extends CrosshairOverlay {
        @Override
        protected void drawHorizontalCrosshair(Graphics2D g2, Rectangle2D dataArea, double y, Crosshair crosshair) {
            super.drawHorizontalCrosshair(g2, dataArea, y, crosshair);
        }

        @Override
        protected void drawVerticalCrosshair(Graphics2D g2, Rectangle2D dataArea, double x, Crosshair crosshair) {
            super.drawVerticalCrosshair(g2, dataArea, x, crosshair);
        }
    }
}