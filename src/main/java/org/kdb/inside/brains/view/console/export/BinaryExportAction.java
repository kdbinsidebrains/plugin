package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.console.TableResultView;

import java.io.IOException;
import java.nio.file.Files;

public class BinaryExportAction extends AnExportAction<VirtualFileWrapper> {
    public BinaryExportAction(String text, ExportingType type, TableResultView resultView, String description) {
        super(text, type, resultView, description);
    }

    @Override
    protected boolean isCancelable() {
        return false;
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, TableResultView view) {
        final FileSaverDescriptor fileSaverDescriptor = new FileSaverDescriptor("Export to KDB Binary", "Exporting data into native KDB IPC format", "data");
        final FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(fileSaverDescriptor, project);
        return saveFileDialog.save((VirtualFile) null, "Table Result");
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, TableResultView view, VirtualFileWrapper file, @NotNull ProgressIndicator indicator) throws IOException {
        indicator.setIndeterminate(true);

        final Object nativeObject = view.getTableResult().getResult().getObject();
        final byte[] serialize = new c().serialize(0, nativeObject, true);
        Files.write(file.getFile().toPath(), serialize);
    }
}
