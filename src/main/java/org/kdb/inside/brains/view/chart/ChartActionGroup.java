package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.view.chart.template.ChartTemplatesService;
import org.kdb.inside.brains.view.chart.template.TemplatesEditorDialog;

import java.util.List;
import java.util.stream.Collectors;

public class ChartActionGroup extends PopupActionGroup {
    private final JBTable table;

    private final AnAction showChartAction;
    private final AnAction showTemplatesManager;

    public ChartActionGroup(JBTable table) {
        super("Show _Chart", icons.KdbIcons.Chart.Icon);//        "Open current table in Excel or compatible application",
        this.table = table;

        showChartAction = new ShowChartAction("Create _Chart", "Open current table in Excel or compatible application", icons.KdbIcons.Chart.Icon, () -> ChartDataProvider.copy(table));
        showTemplatesManager = new DumbAwareAction("Manage _Templates", "Show charting templates manager", KdbIcons.Chart.Templates) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TemplatesEditorDialog.showDialog(e.getProject(), null);
            }
        };
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return new AnAction[]{showChartAction};
        }

        final Project project = e.getProject();
        if (project == null) {
            return new AnAction[]{showChartAction};
        }

        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(showChartAction);

        final ChartDataProvider columns = ChartDataProvider.columns(table);

        final ChartTemplatesService service = ChartTemplatesService.getService(project);
        final List<AnAction> items = service.getTemplates().stream()
                .filter(t -> t.isQuickAction() && t.getConfig().isApplicable(columns))
                .map(t -> new ShowChartAction(t.getName(), t.getDescription(), t.getIcon(), () -> ChartDataProvider.copy(table), t))
                .collect(Collectors.toList());
        if (!items.isEmpty()) {
            group.addSeparator();
            group.addAll(items);
        }

        group.addSeparator();
        group.add(showTemplatesManager);

        return group.getChildren(e);

    }
}
