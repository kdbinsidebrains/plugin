package org.kdb.inside.brains.view.chart;

import org.jfree.chart.JFreeChart;
import org.kdb.inside.brains.KdbType;

public interface ChartTool {
    boolean isEnabled();

    void setEnabled(boolean state);


    void initialize(JFreeChart chart, KdbType domainType);


    default ToolActions getToolActions() {
        return ToolActions.NO_ACTIONS;
    }
}
