package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.NlsActions;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.view.chart.template.ChartTemplate;
import org.kdb.inside.brains.view.console.KdbConsolePanel;

import javax.swing.*;

public class ShowChartAction extends EdtAction {
    private final ChartTemplate template;
    private final ChartDataProvider dataProvider;

    public ShowChartAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon, ChartDataProvider dataProvider) {
        this(text, description, icon, dataProvider, null);
    }

    public ShowChartAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon, ChartDataProvider dataProvider, ChartTemplate template) {
        super(text, description, icon);
        this.template = template;
        this.dataProvider = dataProvider;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final TabInfo data = e.getData(KdbConsolePanel.TAB_INFO_DATA_KEY);
        final String title = data != null ? data.getText() : "KdbInsideBrain Chart";
        if (e.getProject() == null) {
            return;
        }
        new ChartingDialog(e.getProject(), title, dataProvider, template).show();
    }
}
