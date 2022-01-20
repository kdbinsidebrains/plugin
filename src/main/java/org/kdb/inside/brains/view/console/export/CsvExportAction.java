package org.kdb.inside.brains.view.console.export;

import com.google.common.primitives.Primitives;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.TableResultView;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CsvExportAction extends AnExportAction<VirtualFileWrapper> {
    public CsvExportAction(String text, ExportingType type, TableResultView resultView, String description) {
        super(text, type, resultView, description);
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, TableResultView view) {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to CSV", "Exporting data into tab separated file format", "csv");
        final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);
        return saveFileDialog.save((VirtualFile) null, "Table Result");
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, TableResultView view, VirtualFileWrapper file, @NotNull ProgressIndicator indicator) throws IOException {
        exportData(file.getFile(), view, type, indicator);
    }

    private void exportData(File file, TableResultView view, ExportingType type, @NotNull ProgressIndicator indicator) throws IOException {
        final JBTable table = view.getTable();

        final StringBuilder plainStr = new StringBuilder();
        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        if (type.withHeader()) {
            for (int i = ci.reset(); i != -1; i = ci.next()) {
                String val = table.getColumnName(i);
                plainStr.append('"').append(val).append('"').append('\t');
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
        }

        int count = 0;
        double totalCount = ri.count() * ci.count();
        indicator.setIndeterminate(false);
        for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
            ci.reset();
            for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                final Object val = getValueAt(table, view, r, c);
                plainStr.append(val).append('\t');
                indicator.setFraction(count++ / totalCount);
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
        }

        if (indicator.isCanceled()) {
            return;
        }

        // remove the last newline
        plainStr.deleteCharAt(plainStr.length() - 1);

        Files.writeString(file.toPath(), plainStr.toString(), StandardCharsets.UTF_8);
    }

    private Object getValueAt(JBTable table, TableResultView resultView, int r, int c) {
        final Object valueAt = table.getValueAt(r, c);
        if (valueAt == null) {
            return "";
        }

        if (Primitives.isWrapperType(valueAt.getClass())) {
            return valueAt;
        }
        return '"' + resultView.convertValue(valueAt).replace("\"", "\"\"") + '"';
    }
}
