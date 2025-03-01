package org.kdb.inside.brains.lang.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.RootsChangeRescanningInfo;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

@State(name = "QSpecLibrary", storages = {@Storage(value = "kdb-settings.xml")})
public class QSpecLibraryService implements PersistentStateComponent<Element> {
    private static QSpecLibraryService instance;
    private String libraryPath;

    private QSpecLibrary library;
    private String customScript;

    public static QSpecLibraryService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(QSpecLibraryService.class);
        }
        return instance;
    }

    public String getCustomScript() {
        return customScript;
    }

    public void setCustomScript(String customScript) {
        this.customScript = customScript;
    }

    public String getLibraryPath() {
        return libraryPath;
    }

    private static void forceLibraryRescan() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
                final @NotNull Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
                for (@NotNull Project project : openProjects) {
                    ProjectRootManagerEx.getInstanceEx(project).makeRootsChange(EmptyRunnable.INSTANCE, RootsChangeRescanningInfo.RESCAN_DEPENDENCIES_IF_NEEDED);
                }
            });
        });
    }

    public QSpecLibrary getValidLibrary() throws RuntimeConfigurationException {
        if (library == null) {
            throw new RuntimeConfigurationException("QSpec library is not defined");
        }
        return library.validate();
    }

    @Override
    public @Nullable Element getState() {
        Element e = new Element("qspec_library");
        e.setText(customScript);
        e.setAttribute("path", libraryPath);
        return e;
    }

    public void setLibraryPath(String path) {
        if (!Objects.equals(libraryPath, path)) {
            libraryPath = updateLibrary(path);
            forceLibraryRescan();
        }
    }

    @Override
    public void loadState(@NotNull Element state) {
        libraryPath = updateLibrary(state.getAttributeValue("path"));
        customScript = state.getText();
    }

    private String updateLibrary(String path) {
        if (path == null) {
            library = null;
        } else {
            final VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path));
            if (file == null || !file.isDirectory()) {
                library = null;
            } else {
                library = new QSpecLibrary(file);
            }
        }
        return path;
    }
}