package org.kdb.inside.brains.view.chart;

import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;

public record ChartView(@NotNull ChartConfig config, @NotNull JFreeChart chart) {
}