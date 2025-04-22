package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.util.NlsActions;
import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.panel.Overlay;
import org.kdb.inside.brains.view.chart.ChartView;
import org.kdb.inside.brains.view.chart.RendererConfig;
import org.kdb.inside.brains.view.chart.SnapType;

import javax.swing.*;
import java.awt.geom.Rectangle2D;

public interface ChartTool extends Overlay {
    @NotNull
    @Identifier
    String getId();

    @Nullable
    Icon getIcon();

    @NlsActions.ActionText
    String getText();

    @NlsActions.ActionDescription
    String getDescription();


    void chartChanged(ChartView view, SnapType snapType);

    default void chartStyleChanged(JFreeChart chart, RendererConfig config, int datasetIndex, int seriesIndex) {
    }

    default void chartMouseMoved(ChartMouseEvent event, Rectangle2D dataArea) {
    }

    default void chartMouseClicked(ChartMouseEvent event, Rectangle2D dataArea) {
    }

    @NotNull
    default ActionGroup getToolActions() {
        return ActionGroup.EMPTY_GROUP;
    }
}