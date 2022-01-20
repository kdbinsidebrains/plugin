package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.table.JBTable;
import icons.KdbIcons;
import kx.KxConnection;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelExportAction extends AnExportAction<File> {
    private final boolean saveOnDisk;

    public ExcelExportAction(String text, ExportingType type, TableResultView resultView, String description, boolean saveOnDisk) {
        this(text, type, resultView, description, saveOnDisk, KdbIcons.Console.ExportExcel);
    }

    public ExcelExportAction(String text, ExportingType type, TableResultView resultView, String description, boolean saveOnDisk, Icon icon) {
        super(text, type, resultView, description, icon);
        this.saveOnDisk = saveOnDisk;
    }

    @Override
    protected File getExportConfig(Project project, TableResultView view) throws IOException {
        if (saveOnDisk) {
            final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to Excel", "Exporting data into Excel file format", "xlsx");
            final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);

            final VirtualFileWrapper vfw = saveFileDialog.save((VirtualFile) null, "Table Result");
            return vfw == null ? null : vfw.getFile();
        } else {
            return File.createTempFile("kdbinsidebrains_exel_export_", ".xlsx");
        }
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, TableResultView view, File file, @NotNull ProgressIndicator indicator) throws Exception {
        if (exportData(file, view, type, indicator)) {
            Desktop.getDesktop().open(file);
        }
    }

    private boolean exportData(File file, TableResultView view, ExportingType type, @NotNull ProgressIndicator indicator) throws Exception {
        try (final SXSSFWorkbook wb = new SXSSFWorkbook(null, 100, false, true)) {
            final Sheet sheet = wb.createSheet("KDB Exported Data");

            final JBTable table = view.getTable();
            final ExportingType.IndexIterator ri = type.rowsIterator(table);
            final ExportingType.IndexIterator ci = type.columnsIterator(table);

            int index = 0;
            if (type.withHeader()) {
                Row row = sheet.createRow(index++);

                int i = 0;
                for (int c = ci.reset(); c != -1; c = ci.next()) {
                    final String columnName = table.getColumnName(c);
                    row.createCell(i++, CellType.STRING).setCellValue(columnName);
                }
            }

            int count = 0;
            double totalCount = ri.count() * ci.count();
            indicator.setIndeterminate(false);
            for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
                final Row row = sheet.createRow(index++);
                int i = 0;
                for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                    final Object value = table.getValueAt(r, c);
                    if (value instanceof Boolean) {
                        row.createCell(i++, CellType.BOOLEAN).setCellValue((Boolean) value);
                    } else if (value instanceof Number) {
                        if (KxConnection.isNull(value)) {
                            row.createCell(i++, CellType.NUMERIC).setCellValue(view.convertValue(value));
                        } else {
                            row.createCell(i++, CellType.NUMERIC).setCellValue(((Number) value).doubleValue());
                        }
                    } else {
                        row.createCell(i++).setCellValue(view.convertValue(value));
                    }
                    indicator.setFraction(count++ / totalCount);
                }
            }

            if (indicator.isCanceled()) {
                return false;
            }

            try (final FileOutputStream stream = new FileOutputStream(file)) {
                wb.write(stream);
            }
            return true;
        }
    }
}