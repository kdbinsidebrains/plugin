package org.kdb.inside.brains.lang.binding;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class EditorsBindingListener implements FileEditorManagerListener {
    private final EditorsBindingService service;

    public EditorsBindingListener(Project project) {
        service = project.getService(EditorsBindingService.class);
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        service.editorOpened(file);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        service.editorClosed(file);
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        final VirtualFile newFile = event.getNewFile();
        if (newFile != null) {
            service.editorActivated(newFile);
        }
    }
}
