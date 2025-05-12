package org.kdb.inside.brains.view.export;

import com.google.common.primitives.Primitives;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.kdb.inside.brains.UIUtils.saveFile;

public class CsvExportAction extends AnExportAction<VirtualFileWrapper> {
    private static final char SEPARATOR = ',';

    public CsvExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description) {
        super(text, type, dataProvider, description);
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, ExportDataProvider view) {
        return saveFile(project, "Export to CSV", "Exporting data into tab separated file format", "csv", view.getExportName());
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, VirtualFileWrapper file, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) throws Exception {
        final JTable table = dataProvider.getTable();

        final StringBuilder plainStr = new StringBuilder();
        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        if (type.withHeader()) {
            for (int i = ci.reset(); i != -1; i = ci.next()) {
                final String val = table.getColumnName(i);
                final String esc = escapeSpecialCharacters(val);
                plainStr.append(esc).append(SEPARATOR);
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
        }

        int count = 0;
        double totalCount = ri.count() * ci.count();
        indicator.setIndeterminate(false);
        final KdbOutputFormatter formatter = KdbOutputFormatter.getDefault();
        for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
            for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                final String val = getValueAt(table, formatter, r, c);
                final String esc = escapeSpecialCharacters(val);
                plainStr.append(esc).append(SEPARATOR);
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

    private String getValueAt(JTable table, KdbOutputFormatter formatter, int r, int c) {
        final Object valueAt = table.getValueAt(r, c);
        if (valueAt == null) {
            return "";
        }

        if (Primitives.isWrapperType(valueAt.getClass())) {
            return String.valueOf(valueAt);
        }
        return formatter.objectToString(valueAt, false, false);
    }

    private String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}
