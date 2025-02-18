package org.kdb.inside.brains.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.qspec.QSpecLibraryPanel;
import org.kdb.inside.brains.ide.qspec.QSpecLibraryService;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class QSpecConfigurable extends KdbConfigurable {
    public static final String SETTINGS_PAGE_ID = "Kdb.Settings.QSpec";
    private final QSpecLibraryPanel specLibraryPanel = new QSpecLibraryPanel();
    private final QSpecLibraryService libraryService = QSpecLibraryService.getInstance();

    protected QSpecConfigurable() {
        super(SETTINGS_PAGE_ID, "QSpec Testing");
    }

    @Override
    public @Nullable JComponent createComponent() {
        return specLibraryPanel.init(guessActiveProject(), libraryService.getLibrary());
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(libraryService.getLibrary(), specLibraryPanel.getLibrary());
    }

    @Override
    public void apply() throws ConfigurationException {
        libraryService.setLibrary(specLibraryPanel.getLibrary());
    }

    @Override
    public void reset() {
        specLibraryPanel.setLibrary(libraryService.getLibrary());
    }

    Project guessActiveProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                return project;
            }
        }
        return null;
    }
}
