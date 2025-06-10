package org.kdb.inside.brains.view.chart.tools;

import java.util.List;

public interface ModeChartTool<M extends ToolMode> extends ChartTool {
    M getMode();

    void setMode(M mode);

    M findMode(String name);

    List<M> getAvailableModes();
}