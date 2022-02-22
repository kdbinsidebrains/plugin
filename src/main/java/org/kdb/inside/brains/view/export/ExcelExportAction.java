package org.kdb.inside.brains.view.export;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import icons.KdbIcons;
import kx.KxConnection;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelExportAction extends AnExportAction<File> {
    private final boolean saveOnDisk;

    public ExcelExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description, boolean saveOnDisk) {
        this(text, type, dataProvider, description, saveOnDisk, KdbIcons.Console.ExportExcel);
    }

    public ExcelExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description, boolean saveOnDisk, Icon icon) {
        super(text, type, dataProvider, description, icon);
        this.saveOnDisk = saveOnDisk;
    }

    @Override
    protected File getExportConfig(Project project, ExportDataProvider dataProvider) throws IOException {
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
    protected void exportResultView(Project project, ExportingType type, File file, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception {
        if (exportData(file, dataProvider, formatter, type, indicator)) {
            Desktop.getDesktop().open(file);
        }
    }

    private boolean exportData(File file, ExportDataProvider dataProvider, KdbOutputFormatter formatter, ExportingType type, @NotNull ProgressIndicator indicator) throws Exception {
        try (final SXSSFWorkbook wb = new SXSSFWorkbook(null, 100, false, true)) {
            final Sheet sheet = wb.createSheet("KDB Exported Data");

            final JTable table = dataProvider.getTable();
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
                            row.createCell(i++, CellType.NUMERIC).setCellValue(formatter.objectToString(value));
                        } else {
                            row.createCell(i++, CellType.NUMERIC).setCellValue(((Number) value).doubleValue());
                        }
                    } else {
                        row.createCell(i++).setCellValue(formatter.objectToString(value));
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