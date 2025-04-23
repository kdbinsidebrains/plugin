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
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.view.chart.ChartView;
import org.kdb.inside.brains.view.chart.RendererConfig;
import org.kdb.inside.brains.view.chart.SnapType;
import org.kdb.inside.brains.view.chart.tools.AbstractChartTool;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CrosshairTool extends AbstractChartTool {
    private SnapType snapType;

    private DomainCrosshair chDomain;
    private final List<RangeCrosshair> chValues = new ArrayList<>();

    public static final String ID = "CROSSHAIR";

    private static final Drawer DRAWER = new Drawer();

    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance();

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
        chDomain = new DomainCrosshair(plot.getDomainAxisLocation(), plot.getDomainAxis());
        chValues.addAll(snapType == SnapType.NO ? createAxisCrosshair(plot) : createSnapCrosshair(plot));

        fireOverlayChanged();
    }

    @Override
    public void chartStyleChanged(JFreeChart chart, RendererConfig config, int datasetIndex, int seriesIndex) {
        if (chDomain == null || config.color() == null) {
            return;
        }

        for (RangeCrosshair value : chValues) {
            if (value instanceof IndexedCrosshair ic && ic.dataset == datasetIndex && ic.series == seriesIndex) {
                ic.setLabelOutlinePaint(config.color());
                break;
            }
        }

        fireOverlayChanged();
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event, Rectangle2D dataArea) {
        if (chDomain == null) {
            return;
        }

        chDomain.updateValue(event, dataArea, snapType == SnapType.VERTEX ? 0 : 1);

        final double x = chDomain.getValue();
        for (MyCrosshair ch : chValues) {
            ch.updateValue(event, dataArea, x);
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
        DRAWER.drawVerticalCrosshair(g2, dataArea, x, chDomain);

        for (RangeCrosshair ch : chValues) {
            final ValueAxis yAxis = plot.getRangeAxis(ch.dataset);
            final RectangleEdge yAxisEdge = plot.getRangeAxisEdge(ch.dataset);
            ch.paint(g2, dataArea, yAxis, yAxisEdge);
        }
        g2.setClip(savedClip);
    }

    private List<AxisCrosshair> createAxisCrosshair(XYPlot plot) {
        final List<AxisCrosshair> res = new ArrayList<>();
        final int rangeAxisCount = plot.getRangeAxisCount();
        final Map<AxisLocation, AxisCrosshair> r = new HashMap<>();
        for (int i = 0; i < rangeAxisCount; i++) {
            final ValueAxis axis = plot.getRangeAxis(i);

            final AxisLocation location = plot.getRangeAxisLocation(i);

            AxisCrosshair crosshair = r.get(location);
            if (crosshair == null) {
                crosshair = new AxisCrosshair(location, i);
                r.put(location, crosshair);
                res.add(crosshair);
            }

            crosshair.add(axis.getLabel(), i);
        }
        return res;
    }

    private List<RangeCrosshair> createSnapCrosshair(XYPlot plot) {
        final List<RangeCrosshair> res = new ArrayList<>();
        final int datasetCount = plot.getDatasetCount();
        for (int set = 0; set < datasetCount; set++) {
            final AxisLocation location = plot.getRangeAxisLocation(set);

            final XYDataset dataset = plot.getDataset(set);
            final int seriesCount = dataset.getSeriesCount();

            if (dataset instanceof OHLCDataset) {
                for (int ser = 0; ser < seriesCount; ser++) {
                    res.add(new OHLCCrosshair(location, set, ser));
                }
            } else {
                for (int ser = 0; ser < seriesCount; ser++) {
                    final IndexedCrosshair crosshair = new IndexedCrosshair(location, set, ser);
                    final Paint paint = plot.getRenderer(set).getSeriesPaint(ser);
                    if (paint != null) {
                        crosshair.setLabelOutlinePaint(paint);
                    }
                    res.add(crosshair);
                }
            }
        }
        return res;
    }

    private abstract static class MyCrosshair extends Crosshair {
        public MyCrosshair(AxisLocation location) {
            setPaint(CROSSHAIR_PAINT);
            setStroke(STROKE);
            setValue(Double.NaN);
            setLabelVisible(true);
            setLabelPaint(CROSSHAIR_LABEL);
            setLabelOutlineVisible(true);
            setLabelOutlinePaint(CROSSHAIR_OUTLINE);
            setLabelOutlineStroke(CROSSHAIR_STROKE);
            setLabelBackgroundPaint(CROSSHAIR_BACKGROUND);

            setLabelAnchor(convertLocation(location));
            setLabelGenerator(new StandardCrosshairLabelGenerator("  {0}  ", FORMAT));
        }

        protected RectangleAnchor convertLocation(AxisLocation location) {
            if (location == AxisLocation.BOTTOM_OR_LEFT || location == AxisLocation.TOP_OR_LEFT) {
                return RectangleAnchor.LEFT;
            }
            if (location == AxisLocation.BOTTOM_OR_RIGHT || location == AxisLocation.TOP_OR_RIGHT) {
                return RectangleAnchor.RIGHT;
            }
            return null;
        }

        void updateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            setValue(calculateValue(event, dataArea, x));
        }

        abstract double calculateValue(ChartMouseEvent event, Rectangle2D dataArea, double x);
    }

    private abstract static class RangeCrosshair extends MyCrosshair {
        protected final int dataset;

        public RangeCrosshair(AxisLocation location, int dataset) {
            super(location);
            this.dataset = dataset;
        }

        void paint(Graphics2D g2, Rectangle2D dataArea, ValueAxis axis, RectangleEdge edge) {
            final double y = axis.valueToJava2D(getValue(), dataArea, edge);
            DRAWER.drawHorizontalCrosshair(g2, dataArea, y, this);
        }
    }

    private static class DomainCrosshair extends MyCrosshair {
        public DomainCrosshair(AxisLocation location, ValueAxis domainAxis) {
            super(location);

            if (domainAxis instanceof DateAxis da) {
                setLabelGenerator(g -> da.getTickUnit().valueToString(g.getValue()));
            }
        }

        @Override
        protected RectangleAnchor convertLocation(AxisLocation location) {
            if (location == AxisLocation.BOTTOM_OR_LEFT || location == AxisLocation.BOTTOM_OR_RIGHT) {
                return RectangleAnchor.BOTTOM;
            }
            if (location == AxisLocation.TOP_OR_LEFT || location == AxisLocation.TOP_OR_RIGHT) {
                return RectangleAnchor.TOP;
            }
            return null;
        }

        @Override
        double calculateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            return calculateDomainPoint(event, dataArea, x == 0);
        }
    }

    private static class AxisCrosshair extends RangeCrosshair {
        private final List<AxisDetails> details = new ArrayList<>();

        public AxisCrosshair(AxisLocation location, int axisIndex) {
            super(location, axisIndex);
            setLabelGenerator(crosshair -> " " + details.stream().map(d -> d.name + ": " + FORMAT.format(d.value)).collect(Collectors.joining(", ")) + " ");
        }

        public void add(String name, int axesIndex) {
            details.add(new AxisDetails(name, axesIndex));
        }

        @Override
        double calculateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            for (AxisDetails detail : details) {
                detail.value = calculateRangePoint(event, dataArea, detail.axisIndex);
            }
            return calculateRangePoint(event, dataArea, dataset);
        }

        static class AxisDetails {
            final String name;
            final int axisIndex;
            double value;

            public AxisDetails(String name, int axisIndex) {
                this.name = name;
                this.axisIndex = axisIndex;
            }
        }
    }

    private static class IndexedCrosshair extends RangeCrosshair {
        private final int series;

        public IndexedCrosshair(AxisLocation location, int dataset, int series) {
            super(location, dataset);
            this.series = series;
        }

        @Override
        double calculateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            return calculateValuePoint(event, x, dataset, series);
        }
    }

    private static class OHLCCrosshair extends RangeCrosshair {
        private final int series;

        private String label;

        private static final String[] NAMES = new String[]{
                "Open",
                "High",
                "Low",
                "Close",
        };

        private final double[] values = new double[NAMES.length];

        public OHLCCrosshair(AxisLocation location, int dataset, int series) {
            super(location, dataset);
            this.series = series;
            clearValues();
            setLabelGenerator(crosshair -> label + ": " + FORMAT.format(getValue()));
        }

        @Override
        void paint(Graphics2D g2, Rectangle2D dataArea, ValueAxis axis, RectangleEdge edge) {
            for (int i = 0; i < values.length; i++) {
                final double value = values[i];
                if (Double.isNaN(value)) {
                    continue;
                }
                final double y = axis.valueToJava2D(value, dataArea, edge);
                label = NAMES[i];
                setValue(value);
                DRAWER.drawHorizontalCrosshair(g2, dataArea, y, this);
            }
        }

        @Override
        void updateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            if (!(event.getChart().getXYPlot().getDataset(dataset) instanceof OHLCDataset ds)) {
                clearValues();
                return;
            }

            final double[] doubles = calculateOHLCValues(ds, series, x);
            if (doubles == null) {
                return;
            }

            System.arraycopy(doubles, 0, values, 0, values.length);
        }

        void clearValues() {
            Arrays.fill(values, Double.NaN);
        }

        @Override
        double calculateValue(ChartMouseEvent event, Rectangle2D dataArea, double x) {
            return Double.NaN; // not in use
        }
    }

    /**
     * Simple proxy to get rid of CrosshairOverlay extension
     */
    private static class Drawer extends CrosshairOverlay {
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