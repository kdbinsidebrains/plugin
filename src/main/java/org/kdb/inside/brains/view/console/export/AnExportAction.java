package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;

public abstract class AnExportAction extends AnAction {
    private final ExportingType type;

    public AnExportAction(String text, ExportingType type) {
        this(text, null, null, type);
    }

    public AnExportAction(String text, String description, ExportingType type) {
        this(text, description, null, type);
    }

    public AnExportAction(String text, String description, Icon icon, ExportingType type) {
        super(text, description, icon);
        this.type = type;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final TableResultView view = TableResultView.DATA_KEY.getData(e.getDataContext());
        if (view == null || project == null) {
            return;
        }
        performExport(project, view);
    }

    public void performExport(Project project, TableResultView view) {
        if (!type.hasExportingData(view)) {
            return;
        }

        try {
            performAction(project, view, type);
        } catch (Exception ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Data Can't Be Exported");
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final TableResultView view = TableResultView.DATA_KEY.getData(e.getDataContext());
        if (view == null) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(type.hasExportingData(view));
        }
    }

    protected abstract void performAction(Project project, TableResultView view, ExportingType type) throws Exception;
}
