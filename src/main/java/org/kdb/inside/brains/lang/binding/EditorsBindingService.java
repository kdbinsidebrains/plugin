package org.kdb.inside.brains.lang.binding;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.settings.KdbSettingsListener;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EditorsBindingService implements Disposable {
    private boolean disposed;

    private VirtualFile activeFile;
    private KdbInstance activeInstance;

    private EditorsBindingStrategy strategy;

    private final KdbSettingsService settingsService;
    private final MessageBusConnection busConnection;
    private final KdbConnectionManager connectionManager;

    private final Map<VirtualFile, KdbInstance> connections = new HashMap<>();

    private final TheSettingsListener settingsListener = new TheSettingsListener();
    private final TheConnectionListener connectionListener = new TheConnectionListener();

    public EditorsBindingService(Project project) {
        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(connectionListener);

        settingsService = KdbSettingsService.getInstance();
        settingsService.addSettingsListener(settingsListener);
        strategy = settingsService.getConnectionOptions().getBindingStrategy();

        busConnection = project.getMessageBus().connect(this);
        busConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new TheFileEditorManagerListener());
    }

    public void toggleBinding(boolean bind) {
        if (activeInstance == null || activeFile == null || disposed) {
            return;
        }

        if (bind) {
            connections.put(activeFile, activeInstance);
        } else {
            connections.remove(activeFile);
        }
    }

    public boolean isBindable() {
        return activeFile != null && activeInstance != null;
    }

    public boolean hasBinding() {
        return connections.containsKey(activeFile);
    }

    public EditorsBindingStrategy getStrategy() {
        return strategy;
    }

    @Override
    public void dispose() {
        disposed = true;
        settingsService.removeSettingsListener(settingsListener);
        connectionManager.removeConnectionListener(connectionListener);
        busConnection.disconnect();
        connections.clear();
    }


    private void editorClosed(@NotNull VirtualFile file) {
        if (disposed) {
            return;
        }

        connections.remove(file);

        if (Objects.equals(activeFile, file)) {
            activeFile = null;
        }
    }

    private void editorActivated(@NotNull VirtualFile file) {
        if (disposed) {
            return;
        }

        activeFile = file;

        final KdbInstance instance = connections.get(activeFile);
        if (instance != null) {
            connectionManager.activate(instance);
        } else if (activeInstance != null && strategy == EditorsBindingStrategy.TAB_TO_CONNECT) {
            connections.put(activeFile, activeInstance);
        }
    }

    private void instanceActivated(KdbInstance activated) {
        if (disposed) {
            return;
        }

        activeInstance = activated;

        if (activeFile != null && strategy == EditorsBindingStrategy.CONNECT_TO_TAB) {
            if (activated == null) {
                connections.remove(activeFile);
            } else {
                connections.put(activeFile, activeInstance);
            }
        }
    }


    private class TheSettingsListener implements KdbSettingsListener {
        @Override
        public void settingsChanged(KdbSettingsService service, SettingsBean<?> bean) {
            if (!(bean instanceof ExecutionOptions)) {
                return;
            }
            strategy = ((ExecutionOptions) bean).getBindingStrategy();
        }
    }

    private class TheFileEditorManagerListener implements FileEditorManagerListener {
        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            final VirtualFile newFile = event.getNewFile();
            if (newFile != null) {
                editorActivated(newFile);
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            editorClosed(file);
        }
    }

    private class TheConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
            instanceActivated(activated == null ? null : activated.getInstance());
        }
    }
}
