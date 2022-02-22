package org.kdb.inside.brains.view.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
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
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.ide.PopupActionGroup;
import org.kdb.inside.brains.view.chart.line.LineChartBuilder;
import org.kdb.inside.brains.view.chart.ohlc.OHLCChartBuilder;
import org.kdb.inside.brains.view.chart.tools.CrosshairTool;
import org.kdb.inside.brains.view.chart.tools.MeasureTool;
import org.kdb.inside.brains.view.chart.tools.ValuesTool;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartFrame extends FrameWrapper implements DataProvider {
    private BaseChartPanel baseChartPanel;

    private final JBTabs tabs;
    private final JPanel chartPanel = new JPanel(new BorderLayout());

    private final ValuesTool valuesTool;
    private final MeasureTool measureTool;
    private final CrosshairTool crosshairTool;

    private final Splitter splitter = new Splitter(true, 0.75f);

    protected ChartFrame(@Nullable Project project, String title, ChartDataProvider dataProvider) {
        super(project, "KdbInsideBrains-ChartFrameDimension", false, title);

        valuesTool = new ValuesTool(project);
        measureTool = new MeasureTool();
        crosshairTool = new CrosshairTool();

        tabs = createTabs(project, dataProvider);

        final JButton close = new JButton("Close");
        close.addActionListener(e -> close());

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(Borders.customLineTop(JBColor.LIGHT_GRAY));
        buttonPanel.add(close, BorderLayout.EAST);

        final JComponent tabsComponent = tabs.getComponent();

        final JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.add(tabsComponent, BorderLayout.EAST);
        configPanel.add(buttonPanel, BorderLayout.SOUTH);
        configPanel.setBorder(Borders.customLineLeft(JBColor.LIGHT_GRAY));

        final JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(configPanel, BorderLayout.EAST);
        eastPanel.add(createToolbar(), BorderLayout.WEST);

        splitter.setFirstComponent(chartPanel);
        if (valuesTool.isEnabled()) {
            splitter.setSecondComponent(valuesTool.getComponent());
        }

        final JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(eastPanel, BorderLayout.EAST);
        rootPanel.add(splitter, BorderLayout.CENTER);
        rootPanel.setBorder(Borders.compound(Borders.empty(0, 10, 10, 10), Borders.customLine(JBColor.LIGHT_GRAY)));

        setComponent(rootPanel);
        setImage(IconLoader.toImage(KdbIcons.Chart.Icon));
        closeOnEsc();
        configChanged();
    }

    private JBTabs createTabs(Project project, ChartDataProvider dataProvider) {
        final JBTabs tabs = JBTabsFactory.createTabs(project, this);

        final JBTabsPresentation presentation = tabs.getPresentation();
        presentation.setSingleRow(true);
        presentation.setSupportsCompression(true);
        presentation.setTabsPosition(JBTabsPosition.top);

        final List<ChartBuilder> builders = List.of(
                new LineChartBuilder(dataProvider),
                new OHLCChartBuilder(dataProvider)
        );

        final Insets borderInsets = UIManager.getBorder("Button.border").getBorderInsets(new JButton());
        for (ChartBuilder builder : builders) {
            builder.addConfigListener(this::configChanged);

            final JPanel configPanel = builder.getConfigPanel();
            configPanel.setBorder(Borders.empty(0, borderInsets.right));

            final TabInfo info = new TabInfo(configPanel);
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

        baseChartPanel = selectedInfo == null ? null : ((ChartBuilder) selectedInfo.getObject()).createChartPanel();
        measureTool.setChartPanel(baseChartPanel);
        valuesTool.setChartPanel(baseChartPanel);
        crosshairTool.setChartPanel(baseChartPanel);

        chartPanel.removeAll();
        chartPanel.add(baseChartPanel == null ? createEmptyPanel() : baseChartPanel);
        chartPanel.revalidate();
    }

    private static JPanel createEmptyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JBColor.WHITE);
        panel.add(new JLabel("<html><center><h1>There is no data to show.</h1><br><br>Please select the chart type and appropriate configuration.<center></html>"));
        return panel;
    }

    private void measureSelected(boolean state) {
        measureTool.setEnabled(state);
        if (state) {
            pointsSelected(false);
        }
    }

    private void pointsSelected(boolean state) {
        valuesTool.setEnabled(state);
        if (state) {
            splitter.setSecondComponent(valuesTool.getComponent());
            measureSelected(false);
        } else {
            splitter.setSecondComponent(null);
        }
    }

    private JComponent createToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();

        final DynamicToggleAction crosshairAction = new DynamicToggleAction("Crosshair", "Show crosshair lines", KdbIcons.Chart.ToolCrosshair, crosshairTool::isEnabled, crosshairTool::setEnabled);

        group.add(crosshairAction);

        group.addSeparator();

        final ToggleAction measureAction = new DynamicToggleAction("Measure", "Measuring tool", KdbIcons.Chart.ToolMeasure, measureTool::isEnabled, this::measureSelected);
        final ToggleAction pointsAction = new DynamicToggleAction("Points Collector", "Writes each click into a table", KdbIcons.Chart.ToolPoints, valuesTool::isEnabled, this::pointsSelected);
        group.add(measureAction);
        group.add(pointsAction);

        group.addSeparator();
        group.addAll(createChartPanelMenu());

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHARTS_PANEL_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(chartPanel);

        final JComponent actionComponent = actionToolbar.getComponent();
        actionComponent.setBorder(Borders.compound(Borders.customLineLeft(JBColor.LIGHT_GRAY), Borders.empty(5, 3)));

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
            e.getPresentation().setEnabled(baseChartPanel != null);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            baseChartPanel.actionPerformed(new ActionEvent(this, -1, command));
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
