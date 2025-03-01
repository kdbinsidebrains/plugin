package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.wm.WindowManager;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.settings.KdbConfigurable;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;

public class QSpecConfigurable extends KdbConfigurable {
    private JPanel myComponent;

    private JButton specDownloadButton;
    private CustomScriptPanel customScriptPanel;
    private TextFieldWithBrowseButton specFolderField;

    public static final String SETTINGS_PAGE_ID = "Kdb.Settings.QSpec";

    private final QSpecLibraryService libraryService = QSpecLibraryService.getInstance();

    protected QSpecConfigurable() {
        super(SETTINGS_PAGE_ID, "QSpec Testing");
        init(guessActiveProject());
    }

    public JComponent init(@Nullable Project project) {
        UIUtils.initializeFileChooser(project, specFolderField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withRoots(VfsUtil.getUserHomeDir())
                        .withTitle("Nugend QSpec Directory")
                        .withDescription("Select directory where Nugend QSpec package is stored")
        );

        UIUtils.initializerTextBrowseValidator(specFolderField, () -> "Please select directory QSpec folder", () -> "QSpec directory doesn't exist", (s) -> {
            try {
                QSpecLibrary.validatePath(Path.of(s));
                return null;
            } catch (Exception ex) {
                return ex.getMessage();
            }
        });
        specDownloadButton.addActionListener(e -> downloadQspec(project, specFolderField));

        customScriptPanel.init(project);

        return myComponent;
    }

    private void downloadQspec(@Nullable Project project, TextFieldWithBrowseButton field) {
        final QSpecModuleDownloadDialog dialog = new QSpecModuleDownloadDialog(project);
        if (dialog.showAndGet()) {
            field.setText(dialog.getDownloadedPath().toAbsolutePath().toString());
        }
    }

    @Override
    public @Nullable JComponent createComponent() {
        return myComponent;
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(customScriptPanel.getText(), libraryService.getCustomScript()) ||
                !FilenameUtils.equalsNormalizedOnSystem(specFolderField.getText(), libraryService.getLibraryPath());
    }

    @Override
    public void apply() throws ConfigurationException {
        libraryService.setLibraryPath(specFolderField.getText());
        libraryService.setCustomScript(customScriptPanel.getText());
    }

    @Override
    public void reset() {
        specFolderField.setText(libraryService.getLibraryPath());
        customScriptPanel.setText(libraryService.getCustomScript());
    }

    private Project guessActiveProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                return project;
            }
        }
        return null;
    }

    public static void showConfigurable(Project project) {
        // Safe model change here
        ShowSettingsUtil.getInstance().showSettingsDialog(project, QSpecConfigurable.class);
    }
}
