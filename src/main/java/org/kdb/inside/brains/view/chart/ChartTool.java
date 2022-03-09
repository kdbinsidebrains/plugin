package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;

public interface ChartTool {
    void setChart(JFreeChart chart);

    boolean isEnabled();

    void setEnabled(boolean state);
}
