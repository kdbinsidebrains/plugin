package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.ActionGroup;
import org.jfree.chart.JFreeChart;
import org.kdb.inside.brains.KdbType;

public interface ChartTool {
    boolean isEnabled();

    void setEnabled(boolean state);


    void initialize(JFreeChart chart, KdbType domainType);


    default ActionGroup getPopupActions() {
        return ActionGroup.EMPTY_GROUP;
    }
}
