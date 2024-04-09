package org.kdb.inside.brains.view.export;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.console.table.TableMode;
import org.kdb.inside.brains.view.console.table.TableResult;
import org.kdb.inside.brains.view.console.table.TableResultView;
import org.kdb.inside.brains.view.console.table.TabsTableResult;

import javax.swing.*;
import java.awt.*;

public class FlipTableExportAction extends AnExportAction<Boolean> {
    public FlipTableExportAction(ExportDataProvider exportingView) {
        super("Flip Selected Rows", ExportingType.ROWS, exportingView, "Flip and show selected rows in separate dialog", KdbIcons.Console.FlipTable);
    }

    @Override
    protected Boolean getExportConfig(Project project, ExportDataProvider view) {
        return Boolean.TRUE;
    }

    private static void showResultInFrame(Project project, TableResult result) {
        final TableResultView view = new TableResultView(project, TableMode.COMPACT); // Compact mode is required here
        view.showResult(result);

        final ResultDialog dlg = new ResultDialog(project, view);
        var ideFrame = WindowManagerEx.getInstanceEx().getIdeFrame(project);
        if (ideFrame != null) {
            final Rectangle rectangle = ideFrame.suggestChildFrameBounds();
            dlg.setSize((int) rectangle.getWidth(), (int) rectangle.getHeight());
            dlg.setLocation(rectangle.getLocation());
        }
        dlg.show();
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, Boolean cfg, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) {
        final JTable table = dataProvider.getTable();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        final String[] columns = new String[ri.count() + 1];
        columns[0] = "Column";
        for (int r = ri.reset(), j = 1; r != -1; r = ri.next(), j++) {
            columns[j] = "#" + (r + 1);
        }

        final String[] names = new String[ci.count()];
        for (int c = ci.reset(), i = 0; c != -1; c = ci.next(), i++) {
            names[i] = table.getColumnName(c);
        }

        final Object[] values = new Object[ri.count() + 1];
        values[0] = names;
        for (int r = ri.reset(), j = 1; r != -1; r = ri.next(), j++) {
            final Object[] row = new Object[ci.count()];
            for (int c = ci.reset(), i = 0; c != -1; c = ci.next(), i++) {
                row[i] = table.getValueAt(r, c);
            }
            values[j] = row;
        }

        if (indicator.isCanceled()) {
            return;
        }

        final c.Flip data = new c.Flip(new c.Dict(columns, values));
        final TableResult result = TableResult.from(new KdbQuery(""), KdbResult.with(data));
        ApplicationManager.getApplication().invokeLater(() -> {
            final TabsTableResult tabs = TabsTableResult.findParentTabs(table);
            if (tabs == null) {
                showResultInFrame(project, result);
            } else {
                tabs.showTabAfter("Flipped Rows", result);
            }
        });
    }

    private static class ResultDialog extends DialogWrapper {
        private final TableResultView resultView;

        public ResultDialog(@Nullable Project project, TableResultView resultView) {
            super(project, false, DialogWrapper.IdeModalityType.IDE);
            this.resultView = resultView;
            setOKButtonText("Close");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return resultView;
        }

        @Override
        protected Action @NotNull [] createActions() {
            return new Action[]{getOKAction()};
        }
    }
}