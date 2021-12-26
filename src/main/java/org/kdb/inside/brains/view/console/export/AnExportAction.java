package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public abstract class AnExportAction<Config> extends AnAction {
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
    public void update(@NotNull AnActionEvent e) {
        final TableResultView view = getTableResultView(e);
        if (view == null) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(type.hasExportingData(view));
        }
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

        final Config config;
        try {
            config = getExportConfig(project, view);
            if (config == null) {
                return;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Data Can't Be Exported");
            return;
        }

        final String title = "Exporting " + getExportingName(view);
        new Task.Backgroundable(project, title, isCancelable(), PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    exportResultView(project, type, view, config, indicator);
                } catch (Exception ex) {
                    Messages.showErrorDialog(project, ex.getMessage(), "Data Can't Be Exported");
                }
            }
        }.queue();
    }

    protected boolean isCancelable() {
        return true;
    }

    protected abstract Config getExportConfig(Project project, TableResultView view) throws IOException;

    protected abstract void exportResultView(Project project, ExportingType type, TableResultView view, Config config, @NotNull ProgressIndicator indicator) throws Exception;

    private String getExportingName(TableResultView view) {
        final Container parent = view.getParent();
        if (parent instanceof JBTabs) {
            final JBTabs jbTabs = (JBTabs) parent;
            for (TabInfo info : jbTabs.getTabs()) {
                if (info.getObject() == view) {
                    return info.getText();
                }
            }
        }
        return "Table Result";
    }

    @Nullable
    private TableResultView getTableResultView(@NotNull AnActionEvent e) {
        if (resultView != null) {
            return resultView;
        }
        return TableResultView.DATA_KEY.getData(e.getDataContext());
    }
}
