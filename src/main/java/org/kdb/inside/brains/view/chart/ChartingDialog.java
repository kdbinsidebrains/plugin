package org.kdb.inside.brains.view.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.ImageUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.*;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.svg.SVGGraphics2D;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.chart.template.ChartTemplate;
import org.kdb.inside.brains.view.chart.tools.CrosshairTool;
import org.kdb.inside.brains.view.chart.tools.MeasureTool;
import org.kdb.inside.brains.view.chart.tools.ToolToggleAction;
import org.kdb.inside.brains.view.chart.tools.ValuesTool;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartingDialog extends FrameWrapper implements DataProvider {
    private final ValuesTool valuesTool;
    private final MeasureTool measureTool;
    private final CrosshairTool crosshairTool;

    private final BaseChartPanel chartPanel;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel chartLayoutPanel = new JPanel(cardLayout);

    private final Splitter splitter = new Splitter(true, 0.75f);

    private final JPanel rootPanel = new JPanel(new BorderLayout());

    private final Map<Comparable<?>, RendererConfig> rendererConfigMap = new HashMap<>();
    private static final Image IMAGE = IconLoader.toImage(KdbIcons.Chart.Icon);

    private static final String EMPTY_CARD_PANEL_NAME = "EMPTY";
    private static final String CHART_CARD_PANEL_NAME = "CHART";

    protected ChartingDialog(@NotNull Project project, String title, ChartDataProvider dataProvider, ChartTemplate template) {
        super(project, "KdbInsideBrains-Charting", false, title);

        setComponent(rootPanel);

        final ChartOptions chartOptions = KdbSettingsService.getInstance().getChartOptions();

        chartPanel = new BaseChartPanel(chartOptions, this::createPopupMenu);
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                final MouseEvent trigger = event.getTrigger();
                if (trigger.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                if (event.getEntity() instanceof LegendItemEntity item) {
                    changeItemStyle(event.getChart(), item);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });

        valuesTool = new ValuesTool(project, chartPanel, chartOptions);
        measureTool = new MeasureTool(chartPanel, chartOptions);
        crosshairTool = new CrosshairTool(chartPanel, chartOptions);

        final ChartConfigPanel configPanel = new ChartConfigPanel(project, dataProvider, this::configChanged, this::close);

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(configPanel, BorderLayout.EAST);
        eastPanel.add(createToolbar(), BorderLayout.WEST);

        chartLayoutPanel.add(chartPanel, CHART_CARD_PANEL_NAME);
        chartLayoutPanel.add(createEmptyPanel(), EMPTY_CARD_PANEL_NAME);
        cardLayout.show(chartLayoutPanel, EMPTY_CARD_PANEL_NAME);

        splitter.setFirstComponent(chartLayoutPanel);
        if (valuesTool.isEnabled()) {
            splitter.setSecondComponent(valuesTool.getComponent());
        }

        rootPanel.add(eastPanel, BorderLayout.EAST);
        rootPanel.add(splitter, BorderLayout.CENTER);
        rootPanel.setBorder(new CompoundBorder(Borders.empty(0, 10, 10, 10), Borders.customLine(JBColor.LIGHT_GRAY)));

        setImage(IMAGE);
        closeOnEsc();

        configPanel.updateChart(template);
    }

    private static JPanel createEmptyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JBColor.WHITE);
        panel.add(new JLabel("<html><center><h1>There is no data to show.</h1><br><br>Please select the chart style and appropriate configuration.<center></html>"));
        return panel;
    }

    private void changeItemStyle(JFreeChart chart, LegendItemEntity item) {
        final XYPlot xyPlot = chart.getXYPlot();
        final XYDataset dataset = (XYDataset) item.getDataset();

        final Comparable<?> seriesKey = item.getSeriesKey();

        final int datasetIndex = xyPlot.indexOf(dataset);
        final int series = dataset.indexOf(seriesKey);

        final XYItemRenderer renderer = xyPlot.getRenderer(datasetIndex);
        ApplicationManager.getApplication().invokeLater(() -> {
            final RendererConfig config = new RendererConfigDialog(rootPanel, new RendererConfig(renderer, series)).showAndApply();
            if (config != null) {
                config.update(renderer, series);
                rendererConfigMap.put(seriesKey, config);
            }
        });
    }

    private List<ToolActions> createPopupMenu() {
        return Stream.of(crosshairTool, measureTool, valuesTool).filter(ChartTool::isEnabled).map(ChartTool::getToolActions).filter(ToolActions::hasActions).toList();
    }

    private void configChanged(ChartView view) {
        if (view == null) {
            chartPanel.setChart(null);
            cardLayout.show(chartLayoutPanel, EMPTY_CARD_PANEL_NAME);
        } else {
            updateChartRenderer(view);

            final JFreeChart chart = view.chart();
            chartPanel.setChart(chart);
            cardLayout.show(chartLayoutPanel, CHART_CARD_PANEL_NAME);
            List.of(valuesTool, measureTool, crosshairTool).forEach(s -> s.initialize(chart, view.config().getDomainType()));
        }
    }

    private void updateChartRenderer(ChartView view) {
        final XYPlot plot = view.chart().getXYPlot();

        final Map<Integer, XYDataset> datasets = plot.getDatasets();
        for (Map.Entry<Integer, XYDataset> entry : datasets.entrySet()) {
            final XYDataset value = entry.getValue();
            final int seriesCount = value.getSeriesCount();
            for (int i = 0; i < seriesCount; i++) {
                final Comparable<?> seriesKey = value.getSeriesKey(i);
                final RendererConfig rendererConfig = rendererConfigMap.get(seriesKey);
                if (rendererConfig != null) {
                    rendererConfig.update(plot.getRenderer(entry.getKey()), i);
                }
            }
        }
    }

    private void measureSelected(boolean state) {
        if (measureTool.isEnabled() == state) {
            return;
        }

        measureTool.setEnabled(state);
        if (state) {
            valuesSelected(false);
        }
        chartPanel.setDefaultCursor(state);
    }

    private void valuesSelected(boolean state) {
        if (valuesTool.isEnabled() == state) {
            return;
        }
        valuesTool.setEnabled(state);
        if (state) {
            splitter.setSecondComponent(valuesTool.getComponent());
            measureSelected(false);
        } else {
            splitter.setSecondComponent(null);
        }
        chartPanel.setDefaultCursor(state);
    }

    private JComponent createToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();

        group.add(new ToolToggleAction("Crosshair", "Show crosshair lines", KdbIcons.Chart.ToolCrosshair, crosshairTool::isEnabled, crosshairTool::setEnabled));
        group.addSeparator();

        group.add(new ToolToggleAction("Measure", "Measuring tool", KdbIcons.Chart.ToolMeasure, measureTool::isEnabled, this::measureSelected));
        group.add(new ToolToggleAction("Points Collector", "Writes each click into a table", KdbIcons.Chart.ToolPoints, valuesTool::isEnabled, this::valuesSelected));
        group.addSeparator();

        final DefaultActionGroup snapping = new PopupActionGroup("Snapping", KdbIcons.Chart.ToolMagnet);
        snapping.add(new SpanAction("_Disable Snapping", SnapType.NO));
        snapping.add(new SpanAction("Snap to _Line", SnapType.LINE));
        snapping.add(new SpanAction("Snap to _Vertex", SnapType.VERTEX));

        group.add(snapping);

        group.addSeparator();
        group.addAll(createChartPanelMenu());

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHARTS_PANEL_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(chartLayoutPanel);

        final JComponent actionComponent = actionToolbar.getComponent();
        actionComponent.setBorder(new CompoundBorder(Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0), Borders.empty(5, 3)));

        return actionComponent;
    }

    private DefaultActionGroup createChartPanelMenu() {
        final DefaultActionGroup group = new DefaultActionGroup();
        final AnAction action = new EdtAction("_Copy", "Copy the chart", AllIcons.Actions.Copy) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                CopyPasteManager.getInstance().setContents(new ChartingDialog.ChartTransferable());
            }
        };
        action.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, getComponent());
        group.add(action);

        final PopupActionGroup saveAs = new PopupActionGroup("_Save As", AllIcons.Actions.MenuSaveall);
        final EdtAction png = new EdtAction("PNG...", "Save as PNG image", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final FileSaverDescriptor d = new FileSaverDescriptor("Save as PNG Image", "Save chart view as PNG image", "png");
                final VirtualFileWrapper file = FileChooserFactory.getInstance().createSaveFileDialog(d, e.getProject()).save(null);
                if (file == null) {
                    return;
                }
                try (final OutputStream out = new FileOutputStream(file.getFile())) {
                    ChartUtils.writeBufferedImageAsPNG(out, createChartImage());
                } catch (Exception ex) {
                    Messages.showErrorDialog(e.getProject(), "PNG image can't be created: " + ex.getMessage(), "Save as PNG Failed");
                }
            }
        };
        png.registerCustomShortcutSet(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, getComponent());
        saveAs.add(png);

        final EdtAction svg = new EdtAction("SVG...", "Save as SVG image", null) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final FileSaverDescriptor d = new FileSaverDescriptor("Save as SVG Image", "Save chart view as SVG image", "svg");
                final VirtualFileWrapper file = FileChooserFactory.getInstance().createSaveFileDialog(d, e.getProject()).save(null);
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
        svg.registerCustomShortcutSet(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, getComponent());
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

    private class SpanAction extends CheckboxAction {
        private final SnapType snapType;

        private SpanAction(String name, SnapType snapType) {
            super(name);
            this.snapType = snapType;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return chartPanel.getSnapType() == snapType;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            if (state) {
                chartPanel.setSnapType(snapType);
            } else {
                chartPanel.setSnapType(SnapType.NO);
            }
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private String createSVGDocument() {
        final Dimension d = getChartDimension();
        final SVGGraphics2D g2 = new SVGGraphics2D(d.width, d.height);
        g2.setRenderingHint(JFreeChart.KEY_SUPPRESS_SHADOW_GENERATION, true);
        chartPanel.paintComponent(g2);
        return g2.getSVGDocument();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            return valuesTool;
        }
        return super.getData(dataId);
    }

    private BufferedImage createChartImage() {
        final Dimension d = getChartDimension();
        final BufferedImage image = ImageUtil.createImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        chartPanel.paintComponent(g2);
        g2.dispose();
        return image;
    }

    @NotNull
    private Dimension getChartDimension() {
        final Insets insets = chartPanel.getInsets();
        final int w = chartPanel.getWidth() - insets.left - insets.right;
        final int h = chartPanel.getHeight() - insets.top - insets.bottom;
        return new Dimension(w, h);
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
            e.getPresentation().setEnabled(chartPanel.getChart() != null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            chartPanel.actionPerformed(new ActionEvent(this, -1, command));
        }
    }

    private class ChartTransferable implements Transferable {
        /**
         * The data flavor.
         */
        final DataFlavor imageFlavor = new DataFlavor("image/x-java-image; class=java.awt.Image", "Image");

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
            return createChartImage();
        }
    }
}
