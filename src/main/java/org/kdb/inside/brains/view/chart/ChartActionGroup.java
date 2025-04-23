package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.view.chart.template.ChartTemplatesService;
import org.kdb.inside.brains.view.chart.template.TemplatesEditorDialog;
import org.kdb.inside.brains.view.console.table.TableResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ChartActionGroup extends PopupActionGroup {
    private final AnAction[] emptyActionsGroup;
    private final AnAction showTemplatesManager;

    private final Supplier<TableResult> resultSupplier;
    private final ChartTemplatesService templatesService;

    public ChartActionGroup(Project project, Supplier<TableResult> resultSupplier) {
        super("Show _Chart", icons.KdbIcons.Chart.Icon);//        "Open current table in Excel or compatible application",

        this.resultSupplier = resultSupplier;
        this.templatesService = ChartTemplatesService.getService(project);

        this.showTemplatesManager = new EdtAction("Manage _Templates", "Show charting templates manager", KdbIcons.Chart.Templates) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TemplatesEditorDialog.showDialog(e.getProject(), null);
            }
        };
        this.emptyActionsGroup = new AnAction[]{showTemplatesManager};
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        final TableResult tableResult = resultSupplier.get();
        if (tableResult == null) {
            return emptyActionsGroup;
        }

        final ChartDataProvider dataProvider = ChartDataProvider.of(tableResult);

        final List<AnAction> group = new ArrayList<>();
        group.add(new ShowChartAction("Create _Chart", "Open current table in Excel or compatible application", icons.KdbIcons.Chart.Icon, dataProvider));

        final List<AnAction> items = templatesService.getTemplates().stream()
                .filter(t -> t.isQuickAction() && t.getConfig().isApplicable(dataProvider))
                .map(t -> new ShowChartAction(t.getName(), t.getDescription(), t.getIcon(), dataProvider, t))
                .collect(Collectors.toList());

        if (!items.isEmpty()) {
            group.add(Separator.create());
            group.addAll(items);
        }

        group.add(Separator.create());
        group.add(showTemplatesManager);

        return group.toArray(AnAction[]::new);
    }
}
