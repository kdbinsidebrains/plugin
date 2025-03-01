package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.runner.KdbCommonSettingsPanel;
import org.kdb.inside.brains.lang.qspec.CustomScriptPanel;
import org.kdb.inside.brains.lang.qspec.QSpecConfigurable;
import org.kdb.inside.brains.lang.qspec.QSpecLibraryService;

import javax.swing.*;

public class QSpecRunSettingsEditor extends SettingsEditor<QSpecRunConfiguration> {
    private JPanel myComponent;
    private JTextField shouldField;
    private JTextField descField;
    private final QSpecLibraryService libraryService = QSpecLibraryService.getInstance();
    private KdbCommonSettingsPanel commonSettingsPanel;
    private JCheckBox inheritFromCheckBox;
    private CustomScriptPanel customScriptPanel;
    private JCheckBox keepInstanceCheckbox;
    private ActionLink settingsActionLink;
    private JLabel qSpecPath;
    private ActionLink qSpecLibraryLink;

    public QSpecRunSettingsEditor(Project project) {
        customScriptPanel.init(project);
        commonSettingsPanel.init(project);

        customScriptPanel.setEnabled(false);
        inheritFromCheckBox.setSelected(true);

        inheritFromCheckBox.addActionListener(e -> {
            final boolean inherit = inheritFromCheckBox.isSelected();
            customScriptPanel.setEnabled(!inherit);
            if (inherit) {
                customScriptPanel.setText(libraryService.getCustomScript());
            }
        });

        qSpecLibraryLink.addActionListener(e -> showSettings(project));
        settingsActionLink.addActionListener(e -> showSettings(project));

        updateLibraryDetails();
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        return myComponent;
    }

    @Override
    protected void resetEditorFrom(@NotNull QSpecRunConfiguration configuration) {
        descField.setText(configuration.getSuitePattern());
        shouldField.setText(configuration.getTestPattern());
        commonSettingsPanel.resetEditorFrom(configuration);
        keepInstanceCheckbox.setSelected(configuration.isKeepFailed());

        final String cs = configuration.getCustomScript();
        if (cs == null) {
            customScriptPanel.setEnabled(false);
            inheritFromCheckBox.setSelected(true);
            customScriptPanel.setText(libraryService.getCustomScript());
        } else {
            customScriptPanel.setEnabled(true);
            inheritFromCheckBox.setSelected(false);
            customScriptPanel.setText(cs);
        }
    }

    @Override
    protected void applyEditorTo(@NotNull QSpecRunConfiguration configuration) {
        configuration.setSuitePattern(descField.getText());
        configuration.setTestPattern(shouldField.getText());
        configuration.setKeepFailed(keepInstanceCheckbox.isSelected());

        commonSettingsPanel.applyEditorTo(configuration);

        if (inheritFromCheckBox.isSelected()) {
            configuration.setCustomScript(null);
        } else {
            configuration.setCustomScript(customScriptPanel.getText());
        }
    }

    private void showSettings(Project project) {
        QSpecConfigurable.showConfigurable(project);
        updateLibraryDetails();
    }

    private void updateLibraryDetails() {
        qSpecPath.setText(libraryService.getLibraryPath());
        customScriptPanel.setText(libraryService.getCustomScript());

        try {
            libraryService.getValidLibrary();
            qSpecPath.setForeground(JBColor.foreground());
            qSpecPath.setToolTipText("");
        } catch (RuntimeConfigurationException e) {
            qSpecPath.setForeground(JBColor.RED);
            qSpecPath.setToolTipText(e.getLocalizedMessage());
        }
    }
}
