package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;

public interface ChartViewListener {
    @Deprecated
    void configChanged();

    default void chartUpdated(JFreeChart chart) {
        configChanged();
    }

    default void chartCreated(JFreeChart chart) {
        configChanged();
    }
}
