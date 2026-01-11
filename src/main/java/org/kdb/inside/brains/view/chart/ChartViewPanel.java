package org.kdb.inside.brains.view.chart;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.svg.SVGGraphics2D;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.action.PopupActionGroup;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Supplier;

import static org.kdb.inside.brains.UIUtils.saveFile;

public class ChartViewPanel extends ChartPanel {
    private final Supplier<List<ActionGroup>> popupActionsProvider;

    private static final DecimalFormat NUMBER_VALUE_FORMATTER = new DecimalFormat("###,###,###.##");

    private static final JBColor COLOR_GRID = new JBColor(new Color(0xd3d3d4), new Color(0xd3d3d4));

    public ChartViewPanel(Supplier<List<ActionGroup>> popupActionsProvider) {
        super(null, false, false, false, false, false);
        this.popupActionsProvider = popupActionsProvider;

        setFocusable(true);
        setMouseWheelEnabled(true);

        // Set mock menu to process displayPopupMenu
        setPopupMenu(new JPopupMenu());
    }

    public String createSVGDocument() {
        final Dimension d = getChartDimension();
        final SVGGraphics2D g2 = new SVGGraphics2D(d.width, d.height);
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        paintComponent(g2);
        return g2.getSVGDocument();
    }

    public BufferedImage createRawImage() {
        final Dimension d = getChartDimension();
        final BufferedImage image = ImageUtil.createImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        paintComponent(g2);
        g2.dispose();
        return image;
    }

    public DefaultActionGroup createChartActions(JComponent cmp) {
        final DefaultActionGroup group = new DefaultActionGroup();
        final AnAction action = new EdtAction("_Copy", "Copy the chart", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CopyPasteManager.getInstance().setContents(new ChartTransferable());
            }
        };
        action.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, cmp);
        group.add(action);

        final PopupActionGroup saveAs = new PopupActionGroup("_Save As", AllIcons.Actions.MenuSaveall);
        final EdtAction png = new EdtAction("PNG...", "Save as PNG image", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final VirtualFileWrapper file = saveFile(e.getProject(), "Save as PNG Image", "Save chart view as PNG image", "png");
                if (file == null) {
                    return;
                }
                try (final OutputStream out = new FileOutputStream(file.getFile())) {
                    ChartUtils.writeBufferedImageAsPNG(out, createRawImage());
                } catch (Exception ex) {
                    Messages.showErrorDialog(e.getProject(), "PNG image can't be created: " + ex.getMessage(), "Save as PNG Failed");
                }
            }
        };
        png.registerCustomShortcutSet(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, cmp);
        saveAs.add(png);

        final EdtAction svg = new EdtAction("SVG...", "Save as SVG image", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final VirtualFileWrapper file = saveFile(e.getProject(), "Save as SVG Image", "Save chart view as SVG image", "svg");
                if (file == null) {
                    return;
                }

                try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file.getFile()))) {
                    writer.write(createSVGDocument());
                    writer.flush();
                } catch (Exception ex) {
                    Messages.showErrorDialog(e.getProject(), "SVG image can't be created: " + ex.getMessage(), "Save as SVG Failed");
                }
            }
        };
        svg.registerCustomShortcutSet(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, cmp);
        saveAs.add(svg);
        group.add(saveAs);

        group.addSeparator();

        final DefaultActionGroup zoomIn = new PopupActionGroup("Zoom _In", AllIcons.Graph.ZoomIn);
        zoomIn.add(new ChartAction(ChartPanel.ZOOM_IN_BOTH_COMMAND, "_Both Axes", "Zoom in both axes"));
        zoomIn.addSeparator();
        zoomIn.add(new ChartAction(ChartPanel.ZOOM_IN_RANGE_COMMAND, "_Range Axis", "Zoom in only range axis"));
        zoomIn.add(new ChartAction(ChartPanel.ZOOM_IN_DOMAIN_COMMAND, "_Domain Axis", "Zoom in only domain axis"));
        group.add(zoomIn);

        final DefaultActionGroup zoomOut = new PopupActionGroup("Zoom _Out", AllIcons.Graph.ZoomOut);
        zoomOut.add(new ChartAction(ChartPanel.ZOOM_OUT_BOTH_COMMAND, "_Both Axes", "Zoom out both axes"));
        zoomOut.addSeparator();
        zoomOut.add(new ChartAction(ChartPanel.ZOOM_OUT_RANGE_COMMAND, "_Range Axis", "Zoom out only range axis"));
        zoomOut.add(new ChartAction(ChartPanel.ZOOM_OUT_DOMAIN_COMMAND, "_Domain Axis", "Zoom out only domain axis"));
        group.add(zoomOut);

        final DefaultActionGroup zoomReset = new PopupActionGroup("Zoom _Reset", AllIcons.Graph.ActualZoom);
        zoomReset.add(new ChartAction(ChartPanel.ZOOM_RESET_BOTH_COMMAND, "_Both Axes", "Reset the chart zoom"));
        zoomReset.addSeparator();
        zoomReset.add(new ChartAction(ChartPanel.ZOOM_RESET_RANGE_COMMAND, "_Range Axis", "Reset zoom for range axis only"));
        zoomReset.add(new ChartAction(ChartPanel.ZOOM_RESET_DOMAIN_COMMAND, "_Domain Axis", "Reset zoom for domain axis only"));
        group.add(zoomReset);
        return group;
    }

    protected void initializePlot(XYPlot plot) {
        plot.setRangePannable(true);
        plot.setDomainPannable(true);
        plot.setBackgroundPaint(JBColor.background());
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(COLOR_GRID);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(COLOR_GRID);

        final int domainAxisCount = plot.getDomainAxisCount();
        for (int i = 0; i < domainAxisCount; i++) {
            initializeAxis(plot.getDomainAxis(i));
        }

        final int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            initializeAxis(plot.getRangeAxis(i));
        }
    }

    protected void initializeAxis(ValueAxis axis) {
        axis.setLabelPaint(JBColor.foreground());
        axis.setAxisLinePaint(JBColor.foreground());
        axis.setTickLabelPaint(JBColor.foreground());

        if (axis instanceof NumberAxis a) {
            a.setAutoRangeIncludesZero(false);
            a.setNumberFormatOverride(NUMBER_VALUE_FORMATTER);
            // Leads to wrong values display
//        } else if (axis instanceof DateAxis a) {
//            a.setTimeZone(UTC_TIMEZONE);
        }
    }

    @Override
    public void setChart(JFreeChart chart) {
        super.setChart(chart);

        if (chart != null) {
            setMouseZoomable(true);
            initializeChart(chart);
        }
    }

    @NotNull
    private Dimension getChartDimension() {
        final Insets insets = getInsets();
        final int w = getWidth() - insets.left - insets.right;
        final int h = getHeight() - insets.top - insets.bottom;
        return new Dimension(w, h);
    }

    private void initializeChart(JFreeChart chart) {
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
            initializePlot((XYPlot) plot);
        }
    }

    @Override
    protected void displayPopupMenu(int x, int y) {
        final List<ActionGroup> actions = popupActionsProvider.get();
        if (actions.isEmpty()) {
            return;
        }

        final DefaultActionGroup group = new DefaultActionGroup();
        for (ActionGroup action : actions) {
            final String name = action.getTemplateText();
            if (name != null) {
                group.addSeparator(name);
            }
            group.addAll(action);
        }

        if (group.getChildrenCount() == 0) {
            return;
        }

        final ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(null, group, DataManager.getInstance().getDataContext(this), JBPopupFactory.ActionSelectionAid.MNEMONICS, true);
        final Point p = new Point(x, y);
        SwingUtilities.convertPointToScreen(p, this);
        popup.showInScreenCoordinates(this, p);
    }

    private class ChartAction extends EdtAction {
        private final String command;

        public ChartAction(String command, String text, String description) {
            this(command, text, description, null);
        }

        public ChartAction(String command, String text, String description, Icon icon) {
            super(text, description, icon);
            this.command = command;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(getChart() != null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ChartViewPanel.this.actionPerformed(new ActionEvent(this, -1, command));
        }
    }

    private class ChartTransferable implements Transferable {
        /**
         * The data flavor.
         */
        private final DataFlavor imageFlavor = new DataFlavor("image/x-java-image; class=java.awt.Image", "Image");

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{this.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return this.imageFlavor.equals(flavor);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return createRawImage();
        }
    }
}
