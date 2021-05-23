package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import kx.c;
import org.kdb.inside.brains.view.console.TableResultView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class BinaryExportAction extends AnExportAction {
    public BinaryExportAction(String text, ExportingType type, TableResultView resultView, String description) {
        super(text, type, resultView, description);
    }

    @Override
    protected void performAction(Project project, TableResultView view, ExportingType type) throws IOException {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to KDB Binary", "Exporting data into native KDB IPC format", "data");
        final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);
        final VirtualFileWrapper file = saveFileDialog.save((VirtualFile) null, "Table Result");
        if (file != null) {
            exportData(file.getFile(), view, type);
        }

    }

    private void exportData(File file, TableResultView view, ExportingType type) throws IOException {
        final Object nativeObject = view.getTableResult().getResult().getObject();

        final byte[] serialize = new c().serialize(0, nativeObject, true);
        Files.write(file.toPath(), serialize);
    }
}
