package org.kdb.inside.brains.view.export;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import kx.c;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;

import static org.kdb.inside.brains.UIUtils.saveFile;

public class BinaryExportAction extends AnExportAction<VirtualFileWrapper> {
    public BinaryExportAction(String text, ExportingType type, ExportDataProvider dataProvider, String description) {
        super(text, type, dataProvider, description);
    }

    @Override
    protected boolean isCancelable() {
        return false;
    }

    @Override
    protected VirtualFileWrapper getExportConfig(Project project, ExportDataProvider view) {
        return saveFile(project, "Export to KDB Binary", "Exporting data into native KDB IPC format", "kib", view.getExportName());
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, VirtualFileWrapper file, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) throws Exception {
        indicator.setIndeterminate(true);

        final Object nativeObject = dataProvider.getNativeObject();
        final byte[] serialize = new c().serialize(0, nativeObject, true);
        Files.write(file.getFile().toPath(), serialize);
    }
}
