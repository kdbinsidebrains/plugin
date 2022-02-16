package org.kdb.inside.brains.view.console.chart;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.*;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.view.console.chart.line.LineChartBuilder;
import org.kdb.inside.brains.view.console.chart.ohlc.OHLCChartBuilder;
import org.kdb.inside.brains.view.console.chart.tools.CrosshairTool;
import org.kdb.inside.brains.view.console.chart.tools.MeasureTool;
import org.kdb.inside.brains.view.console.chart.tools.ValuesTool;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.intellij.util.ui.JBUI.Borders;

public class ChartFrame extends FrameWrapper {
    private final JBTabs tabs;
    private final JPanel chartPanel = new JPanel(new BorderLayout());

    private final ValuesTool valuesTool = new ValuesTool();
    private final MeasureTool measureTool = new MeasureTool();
    private final CrosshairTool crosshairTool = new CrosshairTool();

    private final Splitter splitter = new Splitter(true, 0.75f);

    protected ChartFrame(@Nullable Project project, String title, ChartDataProvider dataProvider) {
        super(project, "KdbInsideBrains-ChartFrameDimension", false, title);

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
            splitter.setSecondComponent(valuesTool.getPointsComponent());
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

        final BaseChartPanel panel = selectedInfo == null ? null : ((ChartBuilder) selectedInfo.getObject()).createChartPanel();
        measureTool.setChartPanel(panel);
        valuesTool.setChartPanel(panel);
        crosshairTool.setChartPanel(panel);

        chartPanel.removeAll();
        chartPanel.add(panel == null ? createEmptyPanel() : panel);
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
            splitter.setSecondComponent(valuesTool.getPointsComponent());
            measureSelected(false);
        } else {
            splitter.setSecondComponent(null);
        }
    }

    private JComponent createToolbar() {
        final DynamicToggleAction crosshairAction = new DynamicToggleAction("Crosshair", "Show crosshair lines", KdbIcons.Chart.ToolCrosshair, crosshairTool::isEnabled, crosshairTool::setEnabled);
        final ToggleAction measureAction = new DynamicToggleAction("Measure", "Measuring tool", KdbIcons.Chart.ToolMeasure, measureTool::isEnabled, this::measureSelected);
        final ToggleAction pointsAction = new DynamicToggleAction("Points Collector", "Writes each click into a table", KdbIcons.Chart.ToolPoints, valuesTool::isEnabled, this::pointsSelected);

        final DefaultActionGroup group = new DefaultActionGroup();

        group.add(crosshairAction);
        group.addSeparator();
        group.add(measureAction);
        group.addSeparator();
        group.add(pointsAction);

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CHARTS_PANEL_TOOLBAR, group, false);
        actionToolbar.setTargetComponent(chartPanel);

        final JComponent actionComponent = actionToolbar.getComponent();
        actionComponent.setBorder(Borders.compound(Borders.customLineLeft(JBColor.LIGHT_GRAY), Borders.empty(0, 3)));

        return actionComponent;
    }
}
