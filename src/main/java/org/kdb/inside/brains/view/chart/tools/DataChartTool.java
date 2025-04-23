package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.actionSystem.DataProvider;

import javax.swing.*;

public interface DataChartTool extends ChartTool, DataProvider {
    JComponent getComponent();
}
