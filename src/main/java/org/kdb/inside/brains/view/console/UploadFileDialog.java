package org.kdb.inside.brains.view.console;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.core.KdbQuery;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class UploadFileDialog extends DialogWrapper {
    private static final Set<String> TEXT_EXTENTIONS = Set.of("csv", "xml", "txt", "json");
    private final Project project;
    private JBCheckBox textFormat;
    private JBTextField variableName;
    private TextFieldWithBrowseButton browseButton;

    protected UploadFileDialog(@Nullable Project project) {
        super(project, false, IdeModalityType.PROJECT);
        this.project = project;
        setTitle("Uploading File to Instance");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        textFormat = new JBCheckBox("This is text file and will be sent as a list of text lines; bytes array otherwise", true);
        variableName = new JBTextField("." + System.getProperty("user.name") + ".upload");
        browseButton = new TextFieldWithBrowseButton();

        final FileChooserDescriptor d = new FileChooserDescriptor(true, false, false, false, false, false);
        browseButton.addBrowseFolderListener("Uploading File to Instance", "Please select file to be uploaded to the instance", project, d);

        browseButton.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                try {
                    final String text = e.getDocument().getText(e.getOffset(), e.getLength());
                    textFormat.setSelected(isTextFile(text));
                } catch (Exception ignore) {
                    textFormat.setSelected(false);
                }
            }
        });

        final var form = FormBuilder.createFormBuilder();
        form.addLabeledComponent("Variable:", variableName);
        form.addLabeledComponent("Filename:", browseButton);
        form.addComponentToRightColumn(textFormat);
        return form.getPanel();
    }

    public Path getPath() {
        final String text = browseButton.getText();
        return text.isBlank() ? null : Path.of(text);
    }

    public boolean isTextFormat() {
        return textFormat.isSelected();
    }

    public String getVariableName() {
        return variableName.getText();
    }

    private boolean isTextFile(String name) {
        return TEXT_EXTENTIONS.stream().anyMatch(name::endsWith);
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        List<ValidationInfo> r = new ArrayList<>();
        if (!QLanguage.isIdentifier(variableName.getText())) {
            r.add(new ValidationInfo("The name is not valid Q variable identifier", variableName));
        }
        final Path path = getPath();
        if (path == null || Files.notExists(path)) {
            r.add(new ValidationInfo("The file doesn't exist", browseButton));
        }
        return r;
    }

    public KdbQuery createQuery() throws IOException {
        final Path path = getPath();
        final String name = getVariableName();
        final Object data = isTextFormat() ? Files.readAllLines(path).stream().map(String::toCharArray).toArray() : Files.readAllBytes(path);
        return new KdbQuery("set", name, data);
    }
}
