package org.kdb.inside.brains.ide.runner;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.UIUtils;

import javax.swing.*;

public class KdbCommonSettingsPanel extends JPanel {
    @SuppressWarnings("unused")
    private JPanel myRoot;

    private ModulesComboBox myModulesComboBox;
    private TextFieldWithBrowseButton myScriptName;
    private TextFieldWithBrowseButton myWorkingDirectoryField;
    private EnvironmentVariablesTextFieldWithBrowseButton myEnvironmentField;
    private RawCommandLineEditor kdbOptionsField;

    public void init(@NotNull Project project) {
        UIUtils.initializeFileChooser(project, myScriptName,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(QFileType.INSTANCE)
                        .withTitle("Running Q Script")
                        .withDescription("Q Script file or a folder")
        );
        UIUtils.initializerTextBrowseValidator(myScriptName, () -> "Script file can't be empty", () -> "Script file doesn't exist");

        UIUtils.initializeFileChooser(project, myWorkingDirectoryField,
                FileChooserDescriptorFactory.createSingleFolderDescriptor()
                        .withTitle("Working Directory")
                        .withDescription("Working directory")
        );
        UIUtils.initializerTextBrowseValidator(myWorkingDirectoryField, () -> "Working directory can't be empty", () -> "Working directory doesn't exist");
    }

    public void resetEditorFrom(@NotNull KdbRunConfigurationBase configuration) {
        myScriptName.setText(configuration.getScriptName());
        myModulesComboBox.setModules(configuration.getValidModules());
        myModulesComboBox.setSelectedModule(configuration.getExecutionModule());
        myWorkingDirectoryField.setText(configuration.getWorkingDirectory());
        myEnvironmentField.setEnvs(configuration.getEnvs());
        myEnvironmentField.setPassParentEnvs(configuration.isPassParentEnvs());
        kdbOptionsField.setText(configuration.getKdbOptions());
    }

    public void applyEditorTo(@NotNull KdbRunConfigurationBase configuration) {
        configuration.setScriptName(myScriptName.getText());
        configuration.setExecutionModule(myModulesComboBox.getSelectedModule());
        configuration.setWorkingDirectory(myWorkingDirectoryField.getText());
        configuration.setEnvs(myEnvironmentField.getEnvs());
        configuration.setPassParentEnvs(myEnvironmentField.isPassParentEnvs());
        configuration.setKdbOptions(kdbOptionsField.getText().trim());
    }

    public @Nullable Module getSelectedModule() {
        return myModulesComboBox.getSelectedModule();
    }
}
