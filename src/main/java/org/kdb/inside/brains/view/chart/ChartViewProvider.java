package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ChartViewProvider<CP extends JComponent> {
    private final Icon icon;
    private final String name;

    private final List<ChartViewListener> chartViewListeners = new CopyOnWriteArrayList<>();
    protected final ChartDataProvider dataProvider;
    protected CP configPanel;

    public ChartViewProvider(String name, Icon icon, ChartDataProvider dataProvider) {
        this.name = name;
        this.icon = icon;
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

    public Icon getIcon() {
        return icon;
    }

    public String getName() {
        return name;
    }


    public CP getConfigPanel() {
        if (configPanel == null) {
            configPanel = createConfigPanel(dataProvider);
        }
        return configPanel;
    }

    public abstract JFreeChart getJFreeChart();

    protected abstract CP createConfigPanel(ChartDataProvider provider);

    @Deprecated
    protected void processConfigChanged() {
        chartViewListeners.forEach(ChartViewListener::configChanged);
    }
}