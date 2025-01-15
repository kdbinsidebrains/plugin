package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;
import org.kdb.inside.brains.view.chart.types.ChartType;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ChartViewProvider<Panel extends JComponent, Config extends ChartConfig> {
    protected final ChartDataProvider dataProvider;
    protected Panel configPanel;

    private final String name;
    private final ChartType type;
    private final List<ChartViewListener> chartViewListeners = new CopyOnWriteArrayList<>();

    public ChartViewProvider(String name, ChartType type, ChartDataProvider dataProvider) {
        this.name = name;
        this.type = type;
        this.dataProvider = dataProvider;
    }

    public void addConfigListener(ChartViewListener l) {
        if (l != null) {
            chartViewListeners.add(l);
        }
    }

    public void removeConfigListener(ChartViewListener l) {
        if (l != null) {
            chartViewListeners.remove(l);
        }
    }

    public ChartType getType() {
        return type;
    }

    public Icon getIcon() {
        return type.getIcon();
    }

    public String getName() {
        return name;
    }

    public Panel getConfigPanel() {
        if (configPanel == null) {
            configPanel = createConfigPanel(dataProvider);
        }
        return configPanel;
    }

    public abstract Config createChartConfig();

    public abstract void updateChartConfig(Config config);


    public abstract JFreeChart getJFreeChart(Config config);

    protected abstract Panel createConfigPanel(ChartDataProvider provider);

    protected void processConfigChanged() {
        chartViewListeners.forEach(ChartViewListener::configChanged);
    }
}