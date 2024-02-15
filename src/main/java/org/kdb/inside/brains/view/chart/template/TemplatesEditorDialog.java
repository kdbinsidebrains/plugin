package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.options.newEditor.SettingsDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TemplatesEditorDialog extends SettingsDialog {
    TemplatesEditorDialog(@NotNull Project project, @NotNull TemplatesEditorPanel templatePanel) {
        super(project, "KdbChartTemplates", templatePanel, true, false);
    }

    public static void showDialog(final Project project, @Nullable ChartTemplate template) {
        final TemplatesEditorPanel configurable = new TemplatesEditorPanel(project);
        final TemplatesEditorDialog dialog = new TemplatesEditorDialog(project, configurable);
        if (template != null) {
            configurable.selectNodeInTree(template.getName());
        }
        dialog.setSize(700, 500);
        dialog.showAndGet();
    }
}