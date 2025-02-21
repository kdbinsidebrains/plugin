package org.kdb.inside.brains.ide.runner.instance;

import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.RawCommandLineEditor;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.runner.KdbCommonSettingsPanel;

import javax.swing.*;

public class KdbInstanceRunSettingsEditor extends SettingsEditor<KdbInstanceRunConfiguration> {
    private JPanel myComponent;

    private RawCommandLineEditor myScriptArguments;
    private KdbCommonSettingsPanel commonSettingsPanel;

    public KdbInstanceRunSettingsEditor(Project project) {
        commonSettingsPanel.init(project);
    }

    @Override
    protected void resetEditorFrom(@NotNull KdbInstanceRunConfiguration configuration) {
        commonSettingsPanel.resetEditorFrom(configuration);
        myScriptArguments.setText(configuration.getScriptArguments());
    }

    @Override
    protected void applyEditorTo(@NotNull KdbInstanceRunConfiguration configuration) {
        commonSettingsPanel.applyEditorTo(configuration);
        configuration.setScriptArguments(myScriptArguments.getText().trim());
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return myComponent;
    }

    @Override
    protected void disposeEditor() {
        myComponent.setVisible(false);
    }
}