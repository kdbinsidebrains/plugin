package org.kdb.inside.brains.view.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.*;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.ide.PopupActionGroup;
import org.kdb.inside.brains.view.chart.line.LineChartProvider;
import org.kdb.inside.brains.view.chart.ohlc.OHLCChartViewProvider;
import org.kdb.inside.brains.view.chart.tools.CrosshairTool;
import org.kdb.inside.brains.view.chart.tools.MeasureTool;
import org.kdb.inside.brains.view.chart.tools.ValuesTool;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartFrame extends FrameWrapper implements DataProvider {
    private final JBTabs tabs;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel chartLayoutPanel = new JPanel(cardLayout);

    private final ValuesTool valuesTool;
    private final MeasureTool measureTool;
    private final CrosshairTool crosshairTool;

    private final BaseChartPanel chartPanel = new BaseChartPanel(this::createPopupMenu);

    private static final String EMPTY_PANEL = "EMPTY";
    private static final String CHART_PANEL = "CHART";

    private final Splitter splitter = new Splitter(true, 0.75f);

    protected ChartFrame(@Nullable Project project, String title, ChartDataProvider dataProvider) {
        super(project, "KdbInsideBrains-ChartFrameDimension", false, title);

        valuesTool = new ValuesTool(project, chartPanel);
        measureTool = new MeasureTool(chartPanel);
        crosshairTool = new CrosshairTool(chartPanel);

        tabs = createTabs(project, dataProvider);

        final JButton close = new JButton("Close");
        close.addActionListener(e -> close());

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(Borders.customLine(JBColor.LIGHT_GRAY, 1, 0, 0, 0));
        buttonPanel.add(close, BorderLayout.EAST);

        final JComponent tabsComponent = tabs.getComponent();

        final JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.add(tabsComponent, BorderLayout.EAST);
        configPanel.add(buttonPanel, BorderLayout.SOUTH);
        configPanel.setBorder(Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0));

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(configPanel, BorderLayout.EAST);
        eastPanel.add(createToolbar(), BorderLayout.WEST);

        chartLayoutPanel.add(chartPanel, CHART_PANEL);
        chartLayoutPanel.add(createEmptyPanel(), EMPTY_PANEL);

        splitter.setFirstComponent(chartLayoutPanel);
        if (valuesTool.isEnabled()) {
            splitter.setSecondComponent(valuesTool.getComponent());
        }

        final JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(eastPanel, BorderLayout.EAST);
        rootPanel.add(splitter, BorderLayout.CENTER);
        rootPanel.setBorder(new CompoundBorder(Borders.empty(0, 10, 10, 10), Borders.customLine(JBColor.LIGHT_GRAY)));

        setComponent(rootPanel);
        setImage(IconLoader.toImage(KdbIcons.Chart.Icon));
        closeOnEsc();
        configChanged();
    }

    private ActionGroup createPopupMenu() {
        final DefaultActionGroup g = new DefaultActionGroup();
        final List<ChartTool> crosshairTool = List.of(this.crosshairTool, measureTool, valuesTool);
        for (ChartTool tool : crosshairTool) {
            if (!tool.isEnabled()) {
                continue;
            }

            final ActionGroup popupActions = tool.getPopupActions();
            if (popupActions.getChildren(null).length != 0) {
                final String templateText = popupActions.getTemplateText();
                if (templateText != null) {
                    g.addSeparator(templateText);
                }
                g.addAll(popupActions);
            }
        }
        return g;
    }

    private JBTabs createTabs(Project project, ChartDataProvider dataProvider) {
        final JBTabs tabs = JBTabsFactory.createTabs(project, this);

        final JBTabsPresentation presentation = tabs.getPresentation();
        presentation.setSingleRow(true);
        presentation.setSupportsCompression(true);
        presentation.setTabsPosition(JBTabsPosition.top);

        final List<ChartViewProvider<?>> builders = List.of(
                new LineChartProvider(dataProvider),
                new OHLCChartViewProvider(dataProvider)
        );

        final Insets borderInsets = UIManager.getBorder("Button.border").getBorderInsets(new JButton());
        for (ChartViewProvider<?> builder : builders) {
            builder.addConfigListener(this::configChanged);

            final JComponent panel = builder.getConfigPanel();
            panel.setBorder(Borders.empty(0, borderInsets.right));

            final TabInfo info = new TabInfo(panel);
            info.setIcon(builder.getIcon());
            info.setText(builder.getName());
            info.setObject(builder);

            tabs.addTab(info);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                configChanged();
            }
        });
        return tabs;
    }

    private void configChanged() {
        final TabInfo selectedInfo = tabs.getSelectedInfo();
        if (selectedInfo == null) {
            cardLayout.show(chartLayoutPanel, EMPTY_PANEL);
            return;
        }

        final ChartViewProvider<?> provider = (ChartViewProvider<?>) selectedInfo.getObject();

        final JFreeChart chart = provider.getJFreeChart();
        chartPanel.setChart(chart);
        List.of(valuesTool, measureTool, crosshairTool).forEach(s -> s.setChart(chart));

        cardLayout.show(chartLayoutPanel, chart == null ? EMPTY_PANEL : CHART_PANEL);
    }

    private static JPanel createEmptyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JBColor.WHITE);
        panel.add(new JLabel("<html><center><h1>There is no data to show.</h1><br><br>Please select the chart type and appropriate configuration.<center></html>"));
        return panel;
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

        final DynamicToggleAction crosshairAction = new DynamicToggleAction("Crosshair", "Show crosshair lines", KdbIcons.Chart.ToolCrosshair, crosshairTool::isEnabled, crosshairTool::setEnabled);

        group.add(crosshairAction);

        group.addSeparator();

        final ToggleAction measureAction = new DynamicToggleAction("Measure", "Measuring tool", KdbIcons.Chart.ToolMeasure, measureTool::isEnabled, this::measureSelected);
        final ToggleAction pointsAction = new DynamicToggleAction("Points Collector", "Writes each click into a table", KdbIcons.Chart.ToolPoints, valuesTool::isEnabled, this::valuesSelected);
        group.add(measureAction);
        group.add(pointsAction);

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
        actionComponent.setBorder(
                new CompoundBorder(
                        Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0),
                        Borders.empty(5, 3)
                )
        );

        return actionComponent;
    }

    private DefaultActionGroup createChartPanelMenu() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new ChartAction(ChartPanel.COPY_COMMAND, "_Copy", "Copy the chart", AllIcons.Actions.Copy));

        final PopupActionGroup saveAs = new PopupActionGroup("_Save As", AllIcons.Actions.MenuSaveall);
        saveAs.add(new ChartAction("SAVE_AS_PNG", "PNG...", "Save as PNG image"));
        saveAs.add(new ChartAction("SAVE_AS_SVG", "SVG...", "Save as SVG image"));
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
    }

    private class ChartAction extends AnAction {
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

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            return valuesTool;
        }
        return super.getData(dataId);
    }
}
