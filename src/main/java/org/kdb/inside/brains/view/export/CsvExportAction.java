package org.kdb.inside.brains.view.export;

import com.google.common.primitives.Primitives;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CsvExportAction extends AnExportAction<VirtualFileWrapper> {
    public CsvExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description) {
        super(text, type, dataProvider, description);
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, ExportDataProvider dataProvider) {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to CSV", "Exporting data into tab separated file format", "csv");
        final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);
        return saveFileDialog.save((VirtualFile) null, "Table Result");
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, VirtualFileWrapper file, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception {
        final JTable table = dataProvider.getTable();

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
                final Object val = getValueAt(table, formatter, r, c);
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

        Files.writeString(file.getFile().toPath(), plainStr.toString(), StandardCharsets.UTF_8);
    }

    private Object getValueAt(JTable table, KdbOutputFormatter formatter, int r, int c) {
        final Object valueAt = table.getValueAt(r, c);
        if (valueAt == null) {
            return "";
        }

        if (Primitives.isWrapperType(valueAt.getClass())) {
            return valueAt;
        }
        return '"' + formatter.objectToString(valueAt).replace("\"", "\"\"") + '"';
    }
}
