package org.kdb.inside.brains.view.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.testFramework.LightVirtualFile;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.xml.sax.InputSource;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class OpenInEditorAction extends AnExportAction<FileEditorManager> {
    public OpenInEditorAction(String text, ExportDataProvider exportingView) {
        this(text, exportingView, null);
    }

    public OpenInEditorAction(String text, ExportDataProvider exportingView, String description) {
        super(text, ExportingType.SELECTION, exportingView, description, KdbIcons.Console.OpenInEditor);
    }

    @Override
    protected FileEditorManager getExportConfig(Project project, ExportDataProvider view) {
        final JTable table = view.getTable();
        final int i = table.getSelectedColumnCount() * table.getSelectedRowCount();
        if (i > 10 && Messages.showYesNoDialog(project, "You are going to open " + i + " tabs in the editor. Would you like to continue?", "Too Many Cells", "Continue", "Cancel", null) == Messages.NO) {
            return null;
        }
        return FileEditorManager.getInstance(project);
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, FileEditorManager editorManager, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) {
        final JTable table = dataProvider.getTable();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        int count = 0;
        double totalCount = ri.count() * ci.count();
        indicator.setIndeterminate(false);
        final KdbOutputFormatter formatter = KdbOutputFormatter.getDefault();
        for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
            for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                final Object value = table.getValueAt(r, c);
                final String s = formatter.objectToString(value, false, false);
                final String ext = getExtension(s);

                final int row = r;
                final int col = c;
                WriteCommandAction.writeCommandAction(project).run(() -> {
                    final String name = getName(dataProvider, table, row, col);
                    editorManager.openFile(new LightVirtualFile(name + "." + ext, s), true);
                });
                indicator.setFraction(count++ / totalCount);
            }
        }
    }

    private String getName(ExportDataProvider provider, JTable table, int row, int col) {
        return provider.getExportName() + '[' + table.getColumnName(col) + "][" + row + ']';
    }

    private String getExtension(String content) {
        try {
            new ObjectMapper().readTree(content);
            return "json";
        } catch (Exception ignore) {
        }

        try {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(content)));
            return "xml";
        } catch (Exception ignore) {
        }
        return "txt";
    }
}
