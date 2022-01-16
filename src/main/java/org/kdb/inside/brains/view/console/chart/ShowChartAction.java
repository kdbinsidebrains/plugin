package org.kdb.inside.brains.view.console.chart;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.KdbConsolePanel;

import java.util.function.Supplier;

public class ShowChartAction extends AnAction {
    private final Supplier<ChartDataProvider> tableResult;

    public ShowChartAction(String text, String description, Supplier<ChartDataProvider> tableResult) {
        super(text, description, icons.KdbIcons.Chart.Icon);
        this.tableResult = tableResult;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final TabInfo data = e.getData(KdbConsolePanel.TAB_INFO_DATA_KEY);
        final String title = data != null ? data.getText() : "KdbInsideBrain Chart";
        new ChartFrame(e.getProject(), title, tableResult.get()).show();
    }
}
