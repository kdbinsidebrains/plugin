package org.kdb.inside.brains.view.chart.tools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToolMode {
    @NotNull
    String name();

    @NotNull
    String getText();

    @Nullable
    String getDescription();
}