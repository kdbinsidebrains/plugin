package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.ActionLink;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.runner.KdbCommonSettingsPanel;

import javax.swing.*;

public class QSpecRunSettingsEditor extends SettingsEditor<QSpecRunConfiguration> {
    private JPanel myComponent;
    private JTextField expectationField;
    private JTextField specificationField;
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
        commonSettingsPanel.resetEditorFrom(configuration);
        expectationField.setText(configuration.getExpectationPattern());
        specificationField.setText(configuration.getSpecificationPattern());
        keepInstanceCheckbox.setSelected(configuration.isKeepFailedInstance());

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
        commonSettingsPanel.applyEditorTo(configuration);
        configuration.setExpectationPattern(expectationField.getText());
        configuration.setSpecificationPattern(specificationField.getText());
        configuration.setKeepFailedInstance(keepInstanceCheckbox.isSelected());

        if (inheritFromCheckBox.isSelected()) {
            configuration.setCustomScript(null);
        } else {
            configuration.setCustomScript(customScriptPanel.getText());
        }
    }

    private void showSettings(Project project) {
//        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, QSpecConfigurable.class);
        updateLibraryDetails();
//        }
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
