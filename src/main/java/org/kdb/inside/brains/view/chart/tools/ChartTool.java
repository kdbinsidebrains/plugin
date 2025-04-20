package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.util.NlsActions;
import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.panel.Overlay;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ToolActions;

import javax.swing.*;

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


    void initialize(JFreeChart chart, KdbType domainType);


    default ToolActions getToolActions() {
        return ToolActions.NO_ACTIONS;
    }
}
