package org.kdb.inside.brains.view.console.chart;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ChartBuilder {
    private final Icon icon;
    private final String name;

    protected JPanel configPanel;
    protected final ChartDataProvider dataProvider;

    private final List<Runnable> configListeners = new ArrayList<>();

    public ChartBuilder(String name, Icon icon, ChartDataProvider dataProvider) {
        this.name = name;
        this.icon = icon;
        this.dataProvider = dataProvider;
    }

    public void addConfigListener(Runnable runnable) {
        if (runnable != null) {
            configListeners.add(runnable);
        }
    }

    public void removeConfigListener(Runnable runnable) {
        if (runnable != null) {
            configListeners.remove(runnable);
        }
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }


    public JPanel getConfigPanel() {
        if (configPanel == null) {
            configPanel = createConfigPanel();
        }
        return configPanel;
    }

    public abstract BaseChartPanel createChartPanel();


    protected abstract JPanel createConfigPanel();


    protected void processConfigChanged() {
        configListeners.forEach(Runnable::run);
    }
}