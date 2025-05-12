package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.settings.KdbConfigurable;

import javax.swing.*;

import static com.intellij.openapi.util.text.StringUtil.notNullize;

public class QSpecConfigurable extends KdbConfigurable {
    private JPanel myComponent;

    private JButton specDownloadButton;
    private CustomScriptPanel customScriptPanel;
    private TextFieldWithBrowseButton specFolderField;

    public static final String SETTINGS_PAGE_ID = "Kdb.Settings.QSpec";

    private final QSpecLibraryService libraryService = QSpecLibraryService.getInstance();

    protected QSpecConfigurable() {
        super(SETTINGS_PAGE_ID, "QSpec Framework");
        init(ProjectManager.getInstance().getDefaultProject());
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
                QSpecLibrary.validatePath(s);
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
        return !StringUtil.equals(customScriptPanel.getText(), notNullize(libraryService.getCustomScript())) ||
                !FilenameUtils.equalsNormalizedOnSystem(specFolderField.getText(), notNullize(libraryService.getLibraryPath(), ""));
    }

    @Override
    public void apply() throws ConfigurationException {
        final String path = specFolderField.getText();
        libraryService.setLibraryPath(StringUtil.isEmpty(path) ? null : path);

        final String script = customScriptPanel.getText();
        libraryService.setCustomScript(StringUtil.isEmpty(script) ? null : script);
    }

    @Override
    public void reset() {
        specFolderField.setText(libraryService.getLibraryPath());
        customScriptPanel.setText(libraryService.getCustomScript());
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        specFolderField.dispose();
    }

    public static void showConfigurable(Project project) {
        // Safe model change here
        ShowSettingsUtil.getInstance().showSettingsDialog(project, QSpecConfigurable.class);
    }
}
