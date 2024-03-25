package org.kdb.inside.brains.lang.binding;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.settings.KdbSettingsListener;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@State(name = "EditorsBinding", storages = {@Storage(value = "kdb-settings.xml")})
public class EditorsBindingService implements Disposable, PersistentStateComponent<Element> {
    private boolean disposed;

    private VirtualFile activeFile;
    private KdbInstance activeInstance;

    private EditorsBindingStrategy strategy;

    private final Project project;
    private final Map<VirtualFile, KdbInstance> connections = new HashMap<>();

    private final KdbSettingsService settingsService;
    private final KdbConnectionManager connectionManager;

    private final TheSettingsListener settingsListener = new TheSettingsListener();
    private final TheConnectionListener connectionListener = new TheConnectionListener();

    public EditorsBindingService(Project project) {
        this.project = project;

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(connectionListener);

        settingsService = KdbSettingsService.getInstance();
        settingsService.addSettingsListener(settingsListener);
        strategy = settingsService.getExecutionOptions().getBindingStrategy();
    }

    public void toggleBinding(boolean bind) {
        if (activeInstance == null || activeFile == null || disposed) {
            return;
        }

        changeInstance(activeFile, bind ? activeInstance : null);
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
        connections.clear();
    }

    protected void editorOpened(@NotNull VirtualFile file) {
        if (disposed) {
            return;
        }

        editorActivated(file);
    }

    protected void editorClosed(@NotNull VirtualFile file) {
        if (disposed) {
            return;
        }

        changeInstance(file, null);

        if (Objects.equals(activeFile, file)) {
            activeFile = null;
        }
    }

    protected void editorActivated(@NotNull VirtualFile file) {
        if (disposed) {
            return;
        }

        activeFile = file;

        final KdbInstance instance = connections.get(activeFile);
        if (instance != null) {
            connectionManager.activate(instance);
        } else if (activeInstance != null && strategy == EditorsBindingStrategy.TAB_TO_CONNECT) {
            changeInstance(activeFile, activeInstance);
        }
    }

    private void instanceActivated(KdbInstance activated) {
        if (disposed) {
            return;
        }

        activeInstance = activated;

        if (activeFile != null && strategy == EditorsBindingStrategy.CONNECT_TO_TAB) {
            changeInstance(activeFile, activeInstance);
        }
    }

    @Override
    public @Nullable Element getState() {
        final Element e = new Element("bindings");
        for (Map.Entry<VirtualFile, KdbInstance> entry : connections.entrySet()) {
            e.addContent(new Element("binding").setAttribute("url", entry.getKey().getUrl()).setAttribute("instance", entry.getValue().getCanonicalName()));
        }
        return e;
    }

    @Override
    public void loadState(@NotNull Element state) {
        final KdbScopesManager scopesManager = KdbScopesManager.getManager(project);
        final VirtualFileManager fileManager = VirtualFileManager.getInstance();
        for (Element child : state.getChildren()) {
            final String url = child.getAttributeValue("url");
            final String inst = child.getAttributeValue("instance");
            if (url == null || inst == null) {
                continue;
            }
            final VirtualFile vf = fileManager.findFileByUrl(url);
            if (vf == null || !vf.exists()) {
                continue;
            }
            final KdbInstance instance = scopesManager.lookupInstance(inst);
            if (instance == null) {
                continue;
            }
            connections.put(vf, instance);
        }
    }

    private void changeInstance(VirtualFile file, KdbInstance instance) {
        if (instance == null) {
            connections.remove(file);
        } else {
            connections.put(file, instance);
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

    private class TheConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
            instanceActivated(activated == null ? null : activated.getInstance());
        }
    }
}