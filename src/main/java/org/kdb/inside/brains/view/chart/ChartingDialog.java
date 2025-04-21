package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
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
import org.kdb.inside.brains.view.chart.template.ChartTemplate;
import org.kdb.inside.brains.view.chart.tools.ChartTool;
import org.kdb.inside.brains.view.chart.tools.impl.CrosshairTool;
import org.kdb.inside.brains.view.chart.tools.impl.MeasureTool;
import org.kdb.inside.brains.view.chart.tools.impl.ValuesTool;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartingDialog extends FrameWrapper implements DataProvider {
    private final ChartOptions options;

    private final ValuesTool valuesTool;
    private final MeasureTool measureTool;
    private final CrosshairTool crosshairTool;

    private final ChartViewPanel chartPanel;

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

        chartPanel = new ChartViewPanel(options, this::createPopupMenu);
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

        valuesTool = new ValuesTool(project, chartPanel, options);
        measureTool = new MeasureTool(chartPanel, options);
        crosshairTool = new CrosshairTool(chartPanel, options);

        final ChartConfigPanel configPanel = new ChartConfigPanel(project, dataProvider, this::configChanged, this::close);
        Disposer.register(this, configPanel);

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(configPanel, BorderLayout.EAST);
        eastPanel.add(createToolbar(), BorderLayout.WEST);

        chartLayoutPanel.add(chartPanel, CHART_CARD_PANEL_NAME);
        chartLayoutPanel.add(createEmptyPanel(), EMPTY_CARD_PANEL_NAME);
        cardLayout.show(chartLayoutPanel, EMPTY_CARD_PANEL_NAME);

        splitter.setFirstComponent(chartLayoutPanel);
        if (options.isEnabled(valuesTool)) {
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

    private List<ActionGroup> createPopupMenu() {
        return Stream.of(crosshairTool, measureTool, valuesTool).filter(options::isEnabled).map(ChartTool::getToolActions).toList();
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

    private void changeState(ChartTool tool, boolean state) {
        // TODO: remove this code completely
        if (tool instanceof MeasureTool) {
            if (options.isEnabled(valuesTool)) {
                changeState(valuesTool, false);
            }
        } else if (tool instanceof ValuesTool) {
            if (options.isEnabled(measureTool)) {
                changeState(measureTool, false);
            }
            if (state) {
                splitter.setSecondComponent(valuesTool.getComponent());
            } else {
                splitter.setSecondComponent(null);
            }
        }
        options.setEnabled(tool, state);
        chartPanel.repaint(); // overlayChanged?
    }

    private JComponent createToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();

        final DefaultActionGroup snapping = new PopupActionGroup("Snapping", KdbIcons.Chart.ToolMagnet);
        snapping.add(new SpanAction("_Disable Snapping", SnapType.NO));
        snapping.add(new SpanAction("Snap to _Line", SnapType.LINE));
        snapping.add(new SpanAction("Snap to _Vertex", SnapType.VERTEX));

        group.add(snapping);
        group.addSeparator();

        group.add(new ToolToggleAction(crosshairTool));
        group.addSeparator();

        group.add(new ToolToggleAction(measureTool));
        group.add(new ToolToggleAction(valuesTool));

        group.addSeparator();
        group.addAll(chartPanel.createChartActions(getComponent()));

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHARTS_PANEL_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(chartLayoutPanel);

        final JComponent actionComponent = actionToolbar.getComponent();
        actionComponent.setBorder(new CompoundBorder(Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0), Borders.empty(5, 3)));

        return actionComponent;
    }

    private class SpanAction extends CheckboxAction {
        private final SnapType snapType;

        private SpanAction(String name, SnapType snapType) {
            super(name);
            this.snapType = snapType;
            getTemplatePresentation().setKeepPopupOnPerform(KeepPopupOnPerform.Never);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return options.getSnapType() == snapType;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            if (state) {
                options.setSnapType(snapType);
            } else {
                options.setSnapType(SnapType.NO);
            }
            // TODO: update tools with new SnapType
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            return valuesTool;
        }
        return super.getData(dataId);
    }


    public class ToolToggleAction extends EdtToggleAction {
        private final ChartTool tool;

        public ToolToggleAction(@NotNull ChartTool tool) {
            super(tool.getText(), tool.getDescription(), tool.getIcon());
            this.tool = tool;
            changeState(tool, options.isEnabled(tool));
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return options.isEnabled(tool);
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            changeState(tool, state);
        }
    }
}
