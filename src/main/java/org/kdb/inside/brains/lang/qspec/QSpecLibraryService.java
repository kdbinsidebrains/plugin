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
import com.intellij.openapi.util.text.StringUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.util.text.StringUtil.nullize;

@State(name = "QSpecLibrary", storages = {@Storage(value = "kdb-settings.xml")})
public class QSpecLibraryService implements PersistentStateComponent<Element> {
    private String libraryPath;
    private String customScript;

    private QSpecLibrary library;

    private static QSpecLibraryService instance;

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

    protected void forceLibraryRescan() {
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
        QSpecLibrary.validatePath(libraryPath);
        if (library == null) {
            throw new RuntimeConfigurationException("QSpec library is not defined");
        }
        return library.validate();
    }

    @Override
    public @Nullable Element getState() {
        final boolean emptyScript = StringUtil.isEmpty(customScript);
        final boolean emptyPath = StringUtil.isEmpty(libraryPath);
        if (emptyScript && emptyPath) {
            return null;
        }

        final Element e = new Element("qspec_library");
        if (!emptyScript) {
            e.setText(customScript);
        }
        if (!emptyPath) {
            e.setAttribute("path", libraryPath);
        }
        return e;
    }

    @Override
    public void loadState(@NotNull Element state) {
        customScript = nullize(state.getText());
        libraryPath = nullize(state.getAttributeValue("path"));
        library = QSpecLibrary.of(libraryPath);
    }

    public void setLibraryPath(String path) {
        if (!StringUtil.equals(libraryPath, path)) {
            libraryPath = path;
            library = QSpecLibrary.of(libraryPath);
            forceLibraryRescan();
        }
    }
}