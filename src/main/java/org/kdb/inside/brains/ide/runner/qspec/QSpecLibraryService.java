package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "QSpecLibrary", storages = {@Storage(value = "kdb-settings.xml")})
public class QSpecLibraryService implements PersistentStateComponent<Element> {
    private static QSpecLibraryService instance;
    private QSpecLibrary library;

    public static QSpecLibraryService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(QSpecLibraryService.class);
        }
        return instance;
    }

    public QSpecLibrary getLibrary() {
        return library;
    }

    public void setLibrary(QSpecLibrary library) {
        this.library = library;
    }

    @Override
    public @Nullable Element getState() {
        if (library == null) {
            return null;
        }
        Element e = new Element("qpackages");
        library.write(e);
        return e;
    }

    @Override
    public void loadState(@NotNull Element state) {
        library = QSpecLibrary.read(state);
    }
}