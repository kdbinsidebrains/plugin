package org.kdb.inside.brains.view.export;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.io.IOException;

public abstract class AnExportAction<Config> extends AnAction {
    private final ExportingType type;
    private final ExportDataProvider exportingView;

    public AnExportAction(String text, ExportingType type, ExportDataProvider exportingView) {
        this(text, type, exportingView, null);
    }

    public AnExportAction(String text, ExportingType type, ExportDataProvider exportingView, String description) {
        this(text, type, exportingView, description, null);
    }

    public AnExportAction(String text, ExportingType type, ExportDataProvider exportingView, String description, Icon icon) {
        super(text, description, icon);
        this.type = type;
        this.exportingView = exportingView;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final ExportDataProvider view = getTableResultView(e);
        if (view == null) {
            e.getPresentation().setEnabled(false);
        } else {
            e.getPresentation().setEnabled(type.hasExportingData(view));
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final ExportDataProvider view = getTableResultView(e);
        if (view == null || project == null) {
            return;
        }
        performExport(project, view);
    }

    public void performExport(Project project, ExportDataProvider dataProvider) {
        if (!type.hasExportingData(dataProvider)) {
            return;
        }

        final Config config;
        try {
            config = getExportConfig(project, dataProvider);
            if (config == null) {
                return;
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Data Can't Be Exported");
            return;
        }

        final String title = "Exporting " + dataProvider.getExportName();
        new Task.Backgroundable(project, title, isCancelable(), PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final KdbOutputFormatter formatter = KdbOutputFormatter.getInstance();
                    exportResultView(project, type, config, dataProvider, formatter, indicator);
                } catch (Exception ex) {
                    Messages.showErrorDialog(project, ex.getMessage(), "Data Can't Be Exported");
                }
            }
        }.queue();
    }

    protected boolean isCancelable() {
        return true;
    }

    protected abstract Config getExportConfig(Project project, ExportDataProvider view) throws IOException;

    protected abstract void exportResultView(Project project, ExportingType type, Config config, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception;


    @Nullable
    private ExportDataProvider getTableResultView(@NotNull AnActionEvent e) {
        if (exportingView != null) {
            return exportingView;
        }
        return ExportDataProvider.DATA_KEY.getData(e.getDataContext());
    }
}
