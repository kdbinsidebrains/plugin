package org.kdb.inside.brains.view.chart.tools.impl;

import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.text.TextBlockAnchor;
import org.jfree.chart.title.LegendGraphic;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.*;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.ChartView;
import org.kdb.inside.brains.view.chart.RendererConfig;
import org.kdb.inside.brains.view.chart.SnapType;
import org.kdb.inside.brains.view.chart.tools.AbstractChartTool;
import org.kdb.inside.brains.view.chart.tools.ModeChartTool;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LegendTool extends AbstractChartTool implements ModeChartTool<LegendToolMode> {
    private SnapType snapType;
    private ChartType chartType;
    private LegendContainer container;

    private LegendToolMode mode = LegendToolMode.LEFT;

    private final KdbOutputFormatter outputFormatter;

    public static final String ID = "LEGEND";

    private static final String NONE_TEXT = "none";

    private static final int ELEMENTS_GAP = 5;
    private static final int ELEMENTS_GAP_DOUBLE = ELEMENTS_GAP * 2;
    private static final int BOX_OFFSET = 5;
    private static final int BOX_OFFSET_WIDTH = BOX_OFFSET * 3;
    private static final int BOX_PADDING = 4;
    private static final int BOX_PADDING_DOUBLE = BOX_PADDING * 2;

    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;

    public LegendTool(KdbOutputFormatter outputFormatter) {
        super(ID, "Legends", "Show chart legends with current values", KdbIcons.Chart.ToolLegend);
        this.outputFormatter = outputFormatter;
    }

    @Override
    public void chartChanged(ChartView view, SnapType snapType) {
        container = null;
        if (view == null) {
            fireOverlayChanged();
            return;
        }

        this.snapType = snapType;

        final JFreeChart chart = view.chart();
        final XYPlot plot = chart.getXYPlot();
        final ValueAxis axis = plot.getDomainAxis();

        final ValueConverter domainConverter = axis instanceof DateAxis ? this::formatDateValue : this::formatDoubleValue;
        final LegendEntry domain = new LegendEntry(new LegendItem(axis.getLabel()), domainConverter);

        final ChartConfig config = view.config();
        chartType = config.getChartType();

        this.container = new LegendContainer(domain, mode, LegendTitle.DEFAULT_ITEM_FONT, JBColor.foreground());

        if (chartType == ChartType.OHLC) {
            container.append(new LegendEntry(createOHLCItem("Open"), this::formatDoubleValue));
            container.append(new LegendEntry(createOHLCItem("High"), this::formatDoubleValue));
            container.append(new LegendEntry(createOHLCItem("Low"), this::formatDoubleValue));
            container.append(new LegendEntry(createOHLCItem("Close"), this::formatDoubleValue));
            if (config.getRequiredColumns().size() == 6) {
                container.append(new LegendEntry(createOHLCItem("Volume"), this::formatLongValue));
            }
        } else {
            final int datasetCount = plot.getDatasetCount();
            for (int i = 0; i < datasetCount; i++) {
                final XYDataset dataset = plot.getDataset(i);
                final XYItemRenderer renderer = plot.getRenderer(i);
                final int seriesCount = dataset.getSeriesCount();
                for (int j = 0; j < seriesCount; j++) {
                    final LegendItem legendItem = renderer.getLegendItem(i, j);
                    container.append(new LegendEntry(legendItem, this::formatDoubleValue));
                }
            }
        }
        fireOverlayChanged();
    }

    @Override
    public void chartStyleChanged(ChartView chartView, RendererConfig config, int datasetIndex, int seriesIndex) {
        // BlockContainer is read-only so we have to recreate everything
        chartChanged(chartView, snapType);
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event, Rectangle2D dataArea) {
        if (container == null) {
            return;
        }

        final double x = calculateDomainPoint(event, dataArea, snapType == SnapType.VERTEX);

        container.updateValues(x, (domain, entries) -> {
            if (chartType == ChartType.OHLC) {
                final OHLCDataset dataset = (OHLCDataset) event.getChart().getXYPlot().getDataset();
                return calculateOHLCValues(dataset, 0, x);
            } else {
                final double[] res = new double[entries.size()];
                for (int i = 0; i < res.length; i++) {
                    final LegendItem item = entries.get(i).item;
                    res[i] = calculateValuePoint(event, domain, item.getDatasetIndex(), item.getSeriesIndex());
                }
                return res;
            }
        });
        fireOverlayChanged();
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
        if (container == null) {
            return;
        }

        final Font originalFont = g2.getFont();
        try {
            final JFreeChart chart = chartPanel.getChart();
            final Rectangle2D dataArea = chartPanel.getScreenDataArea();

            final Rectangle2D.Double layoutArea = new Rectangle2D.Double(dataArea.getX() + BOX_OFFSET, dataArea.getY() + BOX_OFFSET, dataArea.getWidth() - BOX_OFFSET_WIDTH, dataArea.getHeight() - BOX_OFFSET_WIDTH);
            final Size2D arrange = container.arrange(g2, layoutArea);

            container.boxWidth = Math.max(container.boxWidth, arrange.width + BOX_PADDING_DOUBLE);

            final double boxX;
            if (mode.getHorizontalAlignment() == HorizontalAlignment.RIGHT) {
                boxX = dataArea.getMaxX() - container.boxWidth - BOX_OFFSET;
            } else {
                boxX = dataArea.getMinX() + BOX_OFFSET;
            }

            final double boxY = dataArea.getMaxY() - arrange.height - BOX_OFFSET - BOX_PADDING_DOUBLE;
            final Rectangle2D.Double area = new Rectangle2D.Double(boxX, boxY, container.boxWidth, arrange.height + BOX_PADDING_DOUBLE);

            g2.setPaint(chart.getBackgroundPaint());
            g2.fill(area);

            g2.setPaint(chart.getBorderPaint());
            g2.draw(area);

            container.draw(g2, new Rectangle2D.Double(boxX + BOX_PADDING, boxY + BOX_PADDING, arrange.width, arrange.height));

        } finally {
            g2.setFont(originalFont);
        }
    }

    private @NotNull String formatDateValue(double value) {
        final long v = (long) value;
        if (v == 0) {
            return NONE_TEXT;
        }
        if (v <= ONE_DAY_MILLIS) {
            return outputFormatter.formatTime(new java.sql.Time(v));
        }
        if (v % ONE_DAY_MILLIS == 0) {
            return outputFormatter.formatDate(new java.sql.Date(v));
        }
        return outputFormatter.formatDatetime(new Date(v));
    }

    private @NotNull String formatLongValue(double value) {
        final long v = (long) value;
        if (v == Long.MIN_VALUE) {
            return NONE_TEXT;
        }
        return outputFormatter.formatLong(v);
    }

    private @NotNull String formatDoubleValue(double value) {
        if (Double.isNaN(value)) {
            return NONE_TEXT;
        }
        return outputFormatter.formatDouble(value);
    }

    @Override
    public @NotNull LegendToolMode getMode() {
        return mode;
    }

    @Override
    public void setMode(@NotNull LegendToolMode mode) {
        this.mode = mode;
        if (container != null) {
            container.changeMode(this.mode);
            fireOverlayChanged();
        }
    }

    @Override
    public LegendToolMode findMode(String name) {
        return name == null ? LegendToolMode.LEFT : LegendToolMode.valueOf(name);
    }

    @Override
    public List<LegendToolMode> getAvailableModes() {
        return List.of(LegendToolMode.values());
    }

    @FunctionalInterface
    private interface ValueConverter {
        String generateValue(double v);
    }

    private record LegendEntry(LegendItem item, ValueConverter valueConverter) {
    }

    @FunctionalInterface
    private interface ValueProvider {
        double[] calculateValue(double x, List<LegendEntry> entries);
    }

    private @NotNull LegendItem createOHLCItem(String name) {
        final LegendItem item = new LegendItem(name);
        item.setShapeVisible(false);
        item.setLineVisible(false);
        return item;
    }

    private static final class LegendContainer {
        private double boxWidth;

        private final Font font;
        private final Font fontBold;
        private final Paint paint;

        private final LegendEntry domain;
        private final List<LegendEntry> values = new ArrayList<>();

        private final TextTitle domainText;
        private final List<TextTitle> valuesText = new ArrayList<>();

        private final BlockContainer content;

        public LegendContainer(LegendEntry domain, LegendToolMode options, Font font, Paint paint) {
            this.font = font;
            this.paint = paint;
            fontBold = font.deriveFont(Font.BOLD);

            content = new BlockContainer();
            changeMode(options);

            this.domain = domain;
            this.domainText = insertBlock(domain, true);
        }

        public void changeMode(LegendToolMode mode) {
            final Arrangement arrangement = mode.getHorizontalAlignment() == HorizontalAlignment.CENTER ?
                    new FlowArrangement(HorizontalAlignment.LEFT, VerticalAlignment.CENTER, ELEMENTS_GAP_DOUBLE, 0) :
                    new TableArrangement();
            content.setArrangement(arrangement);
        }

        public void append(LegendEntry entry) {
            values.add(entry);
            valuesText.add(insertBlock(entry, false));
        }

        public void updateValues(double x, ValueProvider valueProvider) {
            domainText.setText(domain.valueConverter.generateValue(x));

            if (Double.isNaN(x)) {
                domainText.setText(NONE_TEXT);
                valuesText.forEach(t -> t.setText(NONE_TEXT));
            } else {
                final int size = values.size();
                final double[] doubles = valueProvider.calculateValue(x, values);
                for (int i = 0; i < size; i++) {
                    valuesText.get(i).setText(values.get(i).valueConverter.generateValue(doubles[i]));
                }
            }
        }

        private @NotNull TextTitle insertBlock(LegendEntry entry, boolean domain) {
            final BlockContainer c = new BlockContainer(new EntryArrangement());
            if (domain) {
                c.add(new EmptyBlock(0, 0));
            } else {
                final LegendGraphic legendGraphic = createLegendGraphic(entry.item);
                legendGraphic.setShapeAnchor(RectangleAnchor.CENTER);
                legendGraphic.setPadding(0, 0, 0, 0);
                c.add(legendGraphic);
            }

            final Font f = domain ? fontBold : font;

            final LabelBlock nameBlock = new LabelBlock(entry.item.getLabel(), f, paint);
            nameBlock.setContentAlignmentPoint(TextBlockAnchor.CENTER_LEFT);
            nameBlock.setPadding(0, ELEMENTS_GAP, 0, 0);
            nameBlock.setTextAnchor(RectangleAnchor.LEFT);
            c.add(nameBlock);

            final TextTitle valueBlock = new TextTitle(NONE_TEXT, f, paint, RectangleEdge.TOP, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, RectangleInsets.ZERO_INSETS);
            valueBlock.setPadding(0, ELEMENTS_GAP, 0, 0);
            c.add(valueBlock);

            content.add(c);

            return valueBlock;
        }

        private LegendGraphic createLegendGraphic(LegendItem legend) {
            final LegendGraphic lg = new LegendGraphic(legend.getShape(), legend.getFillPaint());
            lg.setFillPaintTransformer(legend.getFillPaintTransformer());
            lg.setShapeFilled(legend.isShapeFilled());
            lg.setLine(legend.getLine());
            lg.setLineStroke(legend.getLineStroke());
            lg.setLinePaint(legend.getLinePaint());
            lg.setLineVisible(legend.isLineVisible());
            lg.setShapeVisible(legend.isShapeVisible());
            lg.setShapeOutlineVisible(legend.isShapeOutlineVisible());
            lg.setOutlinePaint(legend.getOutlinePaint());
            lg.setOutlineStroke(legend.getOutlineStroke());
            return lg;
        }

        public Size2D arrange(Graphics2D g2, Rectangle2D area) {
            return content.arrange(g2, new RectangleConstraint(area.getWidth(), area.getHeight()));
        }

        public void draw(Graphics2D g2, Rectangle2D area) {
            content.draw(g2, area);
        }
    }

    private static class EntryArrangement implements Arrangement {
        @Override
        public void add(Block block, Object key) {
            // Not used in this simple implementation
        }

        @Override
        public Size2D arrange(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
            List<?> blocks = container.getBlocks();

            double totalWidth = 0.0;
            double maxHeight = 0.0;
            final int blocksSize = blocks.size();
            Size2D[] sizes = new Size2D[blocksSize];

            // First pass: arrange and measure all blocks
            for (int i = 0; i < blocksSize; i++) {
                Block block = (Block) blocks.get(i);
                Size2D size = block.arrange(g2, RectangleConstraint.NONE);
                sizes[i] = size;
                totalWidth += size.getWidth();
                maxHeight = Math.max(maxHeight, size.getHeight());
            }

            // Second pass: position blocks
            double x = 0.0;
            for (int i = 0; i < blocksSize; i++) {
                Block block = (Block) blocks.get(i);
                Size2D size = sizes[i];
                double y = (maxHeight - size.getHeight()) / 2.0; // vertical center alignment
                block.setBounds(new Rectangle2D.Double(x, y, size.getWidth(), size.getHeight()));
                x += size.getWidth();
            }
            return new Size2D(totalWidth, maxHeight);
        }

        @Override
        public void clear() {
            // Nothing to clear in this simple arrangement
        }
    }

    private static class TableArrangement implements Arrangement {
        @Override
        public void add(Block block, Object key) {
            // Not used
        }

        @Override
        public Size2D arrange(BlockContainer container, Graphics2D g2, RectangleConstraint constraint) {
            List<?> rowContainers = container.getBlocks();
            int cols = 3; // fixed number of columns: legend, label, value
            int rows = rowContainers.size();

            Size2D[][] cellSizes = new Size2D[rows][cols];
            double[] columnWidths = new double[cols];
            double[] rowHeights = new double[rows];

            // Measure sub-blocks and compute column widths and row heights
            for (int row = 0; row < rows; row++) {
                Block rowBlock = (Block) rowContainers.get(row);
                if (!(rowBlock instanceof BlockContainer subContainer) || subContainer.getBlocks().size() != 3) {
                    throw new IllegalArgumentException("Each row block must be a BlockContainer with exactly 3 sub-blocks.");
                }

                for (int col = 0; col < cols; col++) {
                    Block subBlock = (Block) subContainer.getBlocks().get(col);
                    Size2D size = subBlock.arrange(g2, RectangleConstraint.NONE);
                    cellSizes[row][col] = size;
                    columnWidths[col] = Math.max(columnWidths[col], size.getWidth());
                    rowHeights[row] = Math.max(rowHeights[row], size.getHeight());
                }
            }

            double totalWidth = 0;
            for (double w : columnWidths) totalWidth += w;
            double totalHeight = 0;
            for (double h : rowHeights) totalHeight += h;

            // Position sub-blocks inside each row
            double y = 0;
            for (int row = 0; row < rows; row++) {
                BlockContainer subContainer = (BlockContainer) rowContainers.get(row);
                double x = 0;
                final List<?> blocks = subContainer.getBlocks();
                for (int col = 0; col < cols; col++) {
                    Block subBlock = (Block) blocks.get(col);
                    Size2D size = cellSizes[row][col];
                    double offsetX = x + (col == 0 ? (columnWidths[col] - size.getWidth()) / 2.0 : 0); // first column in the middle
                    double offsetY = y + (rowHeights[row] - size.getHeight()) / 2.0;
                    subBlock.setBounds(new Rectangle2D.Double(offsetX, offsetY, size.getWidth(), size.getHeight()));
                    x += columnWidths[col];
                }
                y += rowHeights[row];
            }

            return new Size2D(totalWidth, totalHeight);
        }

        @Override
        public void clear() {
            // Nothing to clear
        }
    }
}