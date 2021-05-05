package org.kdb.inside.brains.ide.runner;

import com.intellij.application.options.ModulesComboBox;
import com.intellij.execution.ui.CommonProgramParametersPanel;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.PanelWithAnchor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.module.KdbModuleType;

import javax.swing.*;

public class KdbProcessRunSettingsEditor extends SettingsEditor<KdbProcessRunConfiguration> implements PanelWithAnchor {
    private JPanel myMainPanel;
    private JComponent anchor;

    private LabeledComponent<ModulesComboBox> moduleComponent;
    private LabeledComponent<TextFieldWithBrowseButton> scriptPathComponent;

    private CommonProgramParametersPanel programParametersPanel;

    private final ConfigurationModuleSelector moduleSelector;

    public KdbProcessRunSettingsEditor(Project project) {
        final TextFieldWithBrowseButton scriptPath = scriptPathComponent.getComponent();

        scriptPath.addBrowseFolderListener(
                "KDB Script Path", "Specify path to KDB script or folder",
                project,
                FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(QFileType.INSTANCE)
        );

        moduleSelector = new ConfigurationModuleSelector(project, moduleComponent.getComponent()) {
            @Override
            public boolean isModuleAccepted(Module module) {
                return KdbModuleType.is(module);
            }
        };

        moduleComponent.getComponent().addActionListener(e -> programParametersPanel.setModuleContext(moduleSelector.getModule()));
        programParametersPanel.setModuleContext(moduleSelector.getModule());

        anchor = UIUtil.mergeComponentsWithAnchor(
                moduleComponent,
                scriptPathComponent,
                programParametersPanel
        );
    }

    @Override
    protected void resetEditorFrom(@NotNull KdbProcessRunConfiguration configuration) {
        moduleSelector.reset(configuration);
        scriptPathComponent.getComponent().setText(configuration.getMainClassName());
        programParametersPanel.reset(configuration);
    }

    @Override
    protected void applyEditorTo(@NotNull KdbProcessRunConfiguration configuration) {
        configuration.setMainClassName(scriptPathComponent.getComponent().getText().trim());
        moduleSelector.applyTo(configuration);
        programParametersPanel.applyTo(configuration);
    }

    @Override
    public JComponent getAnchor() {
        return anchor;
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myMainPanel;
    }

    @Override
    public void setAnchor(@Nullable JComponent anchor) {
        this.anchor = anchor;
        moduleComponent.setAnchor(anchor);
        scriptPathComponent.setAnchor(anchor);
        programParametersPanel.setAnchor(anchor);
    }
}
