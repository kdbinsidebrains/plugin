package org.kdb.inside.brains.view.console.chart;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.*;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.console.chart.line.LineChartBuilder;
import org.kdb.inside.brains.view.console.chart.ohlc.OHLCChartBuilder;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TabsChartFrame extends FrameWrapper {
    private final JBTabs tabs;
    private final JPanel chartPanel = new JPanel(new BorderLayout());

    protected TabsChartFrame(@Nullable Project project, String title, ChartDataProvider dataProvider) {
        super(project, "KdbInsideBrains-ChartFrameDimension", false, title);

        tabs = JBTabsFactory.createTabs(project, this);

        final JBTabsPresentation presentation = tabs.getPresentation();
        presentation.setSingleRow(true);
        presentation.setSupportsCompression(true);
        presentation.setTabsPosition(JBTabsPosition.top);

        final List<ChartBuilder> builders = createBuilders(dataProvider);
        for (ChartBuilder builder : builders) {
            builder.addConfigListener(this::configChanged);

            final TabInfo info = new TabInfo(builder.getConfigPanel());
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

        final JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        rootPanel.add(chartPanel, BorderLayout.CENTER);
        rootPanel.add(tabs.getComponent(), BorderLayout.EAST);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        setComponent(rootPanel);
        setImage(IconLoader.toImage(KdbIcons.Chart.Icon));
        closeOnEsc();
        configChanged();
    }

    private List<ChartBuilder> createBuilders(ChartDataProvider dataProvider) {
        return List.of(
                new LineChartBuilder(dataProvider),
                new OHLCChartBuilder(dataProvider)
        );
    }

    private void configChanged() {
        final TabInfo selectedInfo = tabs.getSelectedInfo();
        final JComponent panel = selectedInfo == null ? null : ((ChartBuilder) selectedInfo.getObject()).createChartPanel();
        chartPanel.removeAll();
        chartPanel.add(panel == null ? createEmptyPanel() : panel, BorderLayout.CENTER);
        chartPanel.revalidate();
    }

    private static JPanel createEmptyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JBColor.WHITE);
        panel.add(new JLabel("<html><center><h1>There is no data to show.</h1><br><br>Please select the chart type and appropriate configuration.<center></html>"));
        return panel;
    }
}
