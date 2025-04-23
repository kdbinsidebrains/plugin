package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;
import org.kdb.inside.brains.view.chart.types.ChartType;

import javax.swing.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class ChartViewProvider<Panel extends JComponent, Config extends ChartConfig> {
    protected Panel configPanel;
    protected final ChartDataProvider dataProvider;

    private final String name;
    private final ChartType type;

    private final Set<ChartConfigListener> configListeners = new CopyOnWriteArraySet<>();

    public ChartViewProvider(String name, ChartType type, ChartDataProvider dataProvider) {
        this.name = name;
        this.type = type;
        this.dataProvider = dataProvider;
    }

    public void addConfigListener(ChartConfigListener l) {
        if (l != null) {
            configListeners.add(l);
        }
    }

    public void removeConfigListener(ChartConfigListener l) {
        if (l != null) {
            configListeners.remove(l);
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

    public ChartView createChartView() {
        final Config config = createChartConfig();

        if (config == null || config.isInvalid()) {
            return null;
        }
        final JFreeChart chart = createJFreeChart(config);
        if (chart == null) {
            return null;
        }
        return new ChartView(config, chart);
    }


    public abstract Config createChartConfig();

    public abstract void updateChartConfig(Config config);


    protected abstract JFreeChart createJFreeChart(Config config);

    protected abstract Panel createConfigPanel(ChartDataProvider provider);


    protected void processConfigChanged() {
        configListeners.forEach(ChartConfigListener::configChanged);
    }
}