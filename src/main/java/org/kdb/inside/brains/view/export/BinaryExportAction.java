package org.kdb.inside.brains.view.export;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import java.io.IOException;
import java.nio.file.Files;

public class BinaryExportAction extends AnExportAction<VirtualFileWrapper> {
    public BinaryExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description) {
        super(text, type, dataProvider, description);
    }

    @Override
    protected boolean isCancelable() {
        return false;
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, ExportDataProvider view) throws IOException {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to KDB Binary", "Exporting data into native KDB IPC format", "data");
        final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);
        return saveFileDialog.save((VirtualFile) null, "Table Result");
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, VirtualFileWrapper file, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception {
        indicator.setIndeterminate(true);

        final Object nativeObject = dataProvider.getNativeObject();
        final byte[] serialize = new c().serialize(0, nativeObject, true);
        Files.write(file.getFile().toPath(), serialize);
    }
}
