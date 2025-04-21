package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.util.NlsActions;
import org.intellij.lang.annotations.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.panel.Overlay;
import org.kdb.inside.brains.KdbType;

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


    @NotNull
    default ActionGroup getToolActions() {
        return ActionGroup.EMPTY_GROUP;
    }
}
