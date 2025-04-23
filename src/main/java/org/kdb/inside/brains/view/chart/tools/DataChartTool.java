package org.kdb.inside.brains.view.chart.tools;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface DataChartTool extends ChartTool { // DataProvider - override only, by some reason
    JComponent getComponent();

    @Nullable
    Object getData(@NotNull @NonNls String dataId);
}
