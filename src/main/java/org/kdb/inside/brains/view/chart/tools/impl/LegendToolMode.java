package org.kdb.inside.brains.view.chart.tools.impl;

import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ui.HorizontalAlignment;
import org.kdb.inside.brains.view.chart.tools.ToolMode;

public enum LegendToolMode implements ToolMode {
    LEFT("Left-bottom", "Show the legend in the left-bottom corner", HorizontalAlignment.LEFT),
    BOTTOM("Bottom", "Show the legend in at the bottom corner", HorizontalAlignment.CENTER),
    RIGHT("Right-bottom", "Show the legend in the right-bottom corner", HorizontalAlignment.RIGHT);

    private final String label;
    private final String description;
    private final HorizontalAlignment horizontalAlignment;

    LegendToolMode(String label, String description, HorizontalAlignment horizontalAlignment) {
        this.label = label;
        this.description = description;
        this.horizontalAlignment = horizontalAlignment;
    }

    @Override
    public @NotNull String getText() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }
}