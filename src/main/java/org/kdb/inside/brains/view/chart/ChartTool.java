package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.ActionGroup;
import org.jfree.chart.JFreeChart;

public interface ChartTool {
    void setChart(JFreeChart chart);

    boolean isEnabled();

    void setEnabled(boolean state);


    default ActionGroup getPopupActions() {
        return ActionGroup.EMPTY_GROUP;
    }
}
