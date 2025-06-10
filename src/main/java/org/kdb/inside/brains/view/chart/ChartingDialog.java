package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.action.EdtToggleAction;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.chart.template.ChartTemplate;
import org.kdb.inside.brains.view.chart.tools.ChartTool;
import org.kdb.inside.brains.view.chart.tools.DataChartTool;
import org.kdb.inside.brains.view.chart.tools.ModeChartTool;
import org.kdb.inside.brains.view.chart.tools.ToolMode;
import org.kdb.inside.brains.view.chart.tools.impl.CrosshairTool;
import org.kdb.inside.brains.view.chart.tools.impl.LegendTool;
import org.kdb.inside.brains.view.chart.tools.impl.MeasureTool;
import org.kdb.inside.brains.view.chart.tools.impl.ValuesTool;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartingDialog extends FrameWrapper implements DataProvider {
    private ChartView activeChartView;

    private final ChartOptions options;
    private final ChartViewPanel chartPanel;

    private final List<ChartTool> chartTools;

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

        options = KdbSettingsService.getInstance().getChartOptions();

        chartPanel = new ChartViewPanel(this::createPopupMenu);
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                final MouseEvent trigger = event.getTrigger();
                if (event.getEntity() instanceof LegendItemEntity item) {
                    if (trigger.getButton() == MouseEvent.BUTTON1) {
                        changeItemStyle(event.getChart(), item);
                    }
                } else {
                    final Rectangle2D area = chartPanel.getScreenDataArea();
                    enabledTools().forEach(t -> t.chartMouseClicked(event, area));
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
                if (event.getEntity() instanceof LegendItemEntity) {
                    return;
                }
                final Rectangle2D area = chartPanel.getScreenDataArea();
                enabledTools().forEach(t -> t.chartMouseMoved(event, area));
            }
        });

        final KdbOutputFormatter outputFormatter = KdbOutputFormatter.getDefault();

        chartTools = List.of(
                new CrosshairTool(),
                new ValuesTool(project, outputFormatter),
                new MeasureTool(chartPanel),
                new LegendTool(outputFormatter)
        );
        chartTools.forEach(this::restoreToolState);
        chartTools.forEach(chartPanel::addOverlay);

        final ChartConfigPanel configPanel = new ChartConfigPanel(project, dataProvider, this::configChanged, this::close);
        Disposer.register(this, configPanel);

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(configPanel, BorderLayout.EAST);
        eastPanel.add(createToolbar(), BorderLayout.WEST);

        chartLayoutPanel.add(chartPanel, CHART_CARD_PANEL_NAME);
        chartLayoutPanel.add(createEmptyPanel(), EMPTY_CARD_PANEL_NAME);
        cardLayout.show(chartLayoutPanel, EMPTY_CARD_PANEL_NAME);

        splitter.setFirstComponent(chartLayoutPanel);

        rootPanel.add(eastPanel, BorderLayout.EAST);
        rootPanel.add(splitter, BorderLayout.CENTER);
        rootPanel.setBorder(new CompoundBorder(Borders.empty(0, 10, 10, 10), Borders.customLine(JBColor.LIGHT_GRAY)));

        setImage(IMAGE);
        closeOnEsc();

        configPanel.updateChart(template);
        enabledTools().forEach(this::invalidateTool);
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

                enabledTools().forEach(t -> t.chartStyleChanged(activeChartView, config, datasetIndex, series));
            }
        });
    }

    private List<ActionGroup> createPopupMenu() {
        return enabledTools().map(ChartTool::getToolActions).toList();
    }

    private void configChanged(ChartView chartView) {
        if (chartView == null) {
            chartPanel.setChart(null);
            cardLayout.show(chartLayoutPanel, EMPTY_CARD_PANEL_NAME);
        } else {
            final JFreeChart chart = chartView.chart();
            updateChartRenderer(chart);
            chartPanel.setChart(chart);
            cardLayout.show(chartLayoutPanel, CHART_CARD_PANEL_NAME);
        }

        activeChartView = chartView;
        invalidateTools();
    }

    private void updateChartRenderer(JFreeChart chart) {
        final XYPlot plot = chart.getXYPlot();

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

    private void changeSnapType(SnapType snapType) {
        options.setSnapType(snapType);
        invalidateTools();
    }

    private void invalidateTools() {
        enabledTools().forEach(t -> t.chartChanged(activeChartView, options.getSnapType()));
        chartPanel.repaint(); // repaint with enabled tool
    }

    private void invalidateTool(ChartTool tool) {
        final boolean state = options.isEnabled(tool);
        if (tool instanceof DataChartTool ct) {
            if (state) {
                final JComponent component = ct.getComponent();
                if (!Objects.equals(splitter.getSecondComponent(), component)) {
                    splitter.setSecondComponent(component);
                }
            } else {
                splitter.setSecondComponent(null);
            }
        }

        tool.chartChanged(state ? activeChartView : null, options.getSnapType());
        chartPanel.repaint(); // repaint with enabled tool
    }

    private JComponent createToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();

        group.add(createSnappingAction());
        group.addSeparator();

        for (ChartTool chartTool : chartTools) {
            group.add(createToolAction(chartTool));
        }

        group.addSeparator();
        group.addAll(chartPanel.createChartActions(getComponent()));

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHARTS_PANEL_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(chartLayoutPanel);

        final JComponent actionComponent = actionToolbar.getComponent();
        actionComponent.setBorder(new CompoundBorder(Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0), Borders.empty(5, 3)));

        return actionComponent;
    }

    private AnAction createToolAction(ChartTool chartTool) {
        if (chartTool instanceof ModeChartTool<?>) {
            @SuppressWarnings("unchecked")
            ToolModeActionGroup<ToolMode> g = new ToolModeActionGroup<>((ModeChartTool<ToolMode>) chartTool);
            return g;
        } else {
            return new ToolToggleAction(chartTool);
        }
    }

    private @NotNull DefaultActionGroup createSnappingAction() {
        final DefaultActionGroup snapping = new PopupActionGroup("Snapping", KdbIcons.Chart.ToolMagnet) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                Toggleable.setSelected(presentation, options.getSnapType() != SnapType.NO);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        };
        snapping.add(new SpanAction("_Disable Snapping", SnapType.NO));
        snapping.add(new SpanAction("Snap to _Line", SnapType.LINE));
        snapping.add(new SpanAction("Snap to _Vertex", SnapType.VERTEX));
        return snapping;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        return enabledTools()
                .filter(t -> t instanceof DataChartTool)
                .map(t -> ((DataChartTool) t).getData(dataId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(super.getData(dataId));
    }

    private Stream<ChartTool> enabledTools() {
        return chartTools.stream().filter(options::isEnabled);
    }

    private void restoreToolState(ChartTool tool) {
        if (tool instanceof ModeChartTool<?>) {
            @SuppressWarnings("unchecked")
            ModeChartTool<ToolMode> ct = (ModeChartTool<ToolMode>) tool;
            final ToolMode mode = options.getMode(ct);
            if (mode != null) {
                ct.setMode(mode);
            }
        }
    }

    private class SpanAction extends EdtToggleAction {
        private final SnapType snapType;

        private SpanAction(String name, SnapType snapType) {
            super(name);
            this.snapType = snapType;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return options.getSnapType() == snapType;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            changeSnapType(state ? snapType : SnapType.NO);
        }
    }

    private class ToolToggleAction extends EdtToggleAction {
        private final ChartTool tool;

        public ToolToggleAction(@NotNull ChartTool tool) {
            super(tool.getText(), tool.getDescription(), tool.getIcon());
            this.tool = tool;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return options.isEnabled(tool);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            options.setEnabled(tool, state);
            invalidateTool(tool);
        }
    }

    private class ToolModeActionGroup<M extends ToolMode> extends PopupActionGroup {
        @NotNull
        private final ModeChartTool<M> chartTool;

        public ToolModeActionGroup(ModeChartTool<M> chartTool) {
            super(chartTool.getId(), chartTool.getIcon());
            this.chartTool = chartTool;

            add(new ToolModeAction<>(chartTool));
            for (M mode : chartTool.getAvailableModes()) {
                add(new ToolModeAction<>(chartTool, mode));
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            Toggleable.setSelected(presentation, options.isEnabled(chartTool));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    private class ToolModeAction<M extends ToolMode> extends EdtToggleAction {
        private final M mode;
        private final ModeChartTool<M> tool;

        public ToolModeAction(@NotNull ModeChartTool<M> tool) {
            super("Disable", "Disable this charting tool", null);
            this.tool = tool;
            mode = null;
        }

        public ToolModeAction(@NotNull ModeChartTool<M> tool, M mode) {
            super(mode.getText(), mode.getDescription(), null);
            this.tool = tool;
            this.mode = mode;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return options.isMode(tool, mode);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            if (mode != null) {
                tool.setMode(mode);
            }
            options.setModel(tool, mode);
            invalidateTool(tool);
        }
    }
}
