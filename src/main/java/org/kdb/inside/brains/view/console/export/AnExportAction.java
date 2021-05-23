package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;

public abstract class AnExportAction extends AnAction {
    private final ExportingType type;
    private final TableResultView resultView;

    public AnExportAction(String text, ExportingType type, TableResultView resultView) {
        this(text, type, resultView, null);
    }

    public AnExportAction(String text, ExportingType type, TableResultView resultView, String description) {
        this(text, type, resultView, description, null);
    }

    public AnExportAction(String text, ExportingType type, TableResultView resultView, String description, Icon icon) {
        super(text, description, icon);
        this.type = type;
        this.resultView = resultView;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final TableResultView view = getTableResultView(e);
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
        final TableResultView view = getTableResultView(e);
        if (view == null) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(type.hasExportingData(view));
        }
    }

    @Nullable
    private TableResultView getTableResultView(@NotNull AnActionEvent e) {
        if (resultView != null) {
            return resultView;
        }
        return TableResultView.DATA_KEY.getData(e.getDataContext());
    }

    protected abstract void performAction(Project project, TableResultView view, ExportingType type) throws Exception;
}
