package org.kdb.inside.brains.ide.qspec;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.UIUtils;

import javax.swing.*;

public class QSpecLibraryPanel {
    private static final String DEFAULT_TEXT = """
            / Q below code that will be executed before running any QSpec tests.
            / You can use it to initialize any required variables or load external files
            / Please use only system commands, not slashed one. Use semicolons to split sentences.
            """;
    private JPanel myComponent;
    private JScrollPane scriptPanel;
    private LanguageTextField scriptTextArea;
    private JButton specDownloadButton;
    private TextFieldWithBrowseButton specFolderField;

    public QSpecLibraryPanel() {
    }

    public JComponent init(@Nullable Project project, @Nullable QSpecLibrary library) {
        scriptTextArea = new LanguageTextField(QLanguage.INSTANCE, project, DEFAULT_TEXT, false);

        UIUtils.initializeFileChooser(project, specFolderField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withRoots(VfsUtil.getUserHomeDir())
                        .withTitle("Nugend QSpec Directory")
                        .withDescription("Select directory where Nugend QSpec package is stored")
        );

        UIUtils.initializerTextBrowseValidator(specFolderField, () -> "Please select directory QSpec folder", () -> "QSpec directory doesn't exist", (s) -> {
            try {
                QSpecLibrary.validate(s);
                return null;
            } catch (Exception ex) {
                return ex.getMessage();
            }
        });
        specDownloadButton.addActionListener(e -> downloadQspec(project, specFolderField));

        scriptPanel.setViewportView(scriptTextArea);

        setLibrary(library);

        return myComponent;
    }

    private void downloadQspec(@Nullable Project project, TextFieldWithBrowseButton field) {
        final QSpecModuleDownloadDialog dialog = new QSpecModuleDownloadDialog(project);
        if (dialog.showAndGet()) {
            field.setText(dialog.getDownloadedPath().toAbsolutePath().toString());
        }
    }

    public QSpecLibrary getLibrary() {
        return new QSpecLibrary(specFolderField.getText(), scriptTextArea.getText());
    }

    public void setLibrary(QSpecLibrary library) {
        if (library != null) {
            scriptTextArea.setText(library.script());
            specFolderField.setText(library.specFolder());
        } else {
            specFolderField.setText("");
            scriptTextArea.setText(DEFAULT_TEXT);
        }
    }

    public void setEnabled(boolean enabled) {
        specFolderField.setEnabled(enabled);
        specDownloadButton.setEnabled(enabled);
        scriptTextArea.setEnabled(enabled);
    }
}