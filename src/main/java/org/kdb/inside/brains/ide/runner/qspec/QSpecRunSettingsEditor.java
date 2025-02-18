package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.ide.DataManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.qspec.QSpecLibraryPanel;
import org.kdb.inside.brains.ide.qspec.QSpecLibraryService;
import org.kdb.inside.brains.ide.runner.KdbCommonSettingsPanel;
import org.kdb.inside.brains.settings.QSpecConfigurable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class QSpecRunSettingsEditor extends SettingsEditor<QSpecRunConfiguration> {
    private final QSpecLibraryService libraryService = QSpecLibraryService.getInstance();
    private JPanel myComponent;
    private JTextField expectationField;
    private JTextField specificationField;
    private QSpecLibraryPanel qSpecLibraryPanel;
    private KdbCommonSettingsPanel commonSettingsPanel;
    private JCheckBox inheritFromCheckBox;
    private HyperlinkLabel settingsActionLink;

    public QSpecRunSettingsEditor(Project project) {
        qSpecLibraryPanel.init(project, libraryService.getLibrary());
        commonSettingsPanel.init(project);

        qSpecLibraryPanel.setEnabled(false);
        inheritFromCheckBox.setSelected(true);

        inheritFromCheckBox.addActionListener(e -> {
            final boolean inherit = inheritFromCheckBox.isSelected();
            qSpecLibraryPanel.setEnabled(!inherit);
            if (inherit) {
                qSpecLibraryPanel.setLibrary(libraryService.getLibrary());
            }
        });

        settingsActionLink.setVisible(true);
        settingsActionLink.setHyperlinkText("settings");
        settingsActionLink.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                DataManager.getInstance().getDataContextFromFocusAsync().onSuccess(ctx -> {
                    if (ctx == null) {
                        return;
                    }
                    Settings settings = Settings.KEY.getData(ctx);
                    if (settings != null) {
                        settings.select(settings.find(QSpecConfigurable.class));
                    }
                });
            }
        });
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

        final boolean inheritLibrary = configuration.isInheritLibrary();
        qSpecLibraryPanel.setEnabled(!inheritLibrary);
        inheritFromCheckBox.setSelected(inheritLibrary);
        if (inheritLibrary) {
            qSpecLibraryPanel.setLibrary(libraryService.getLibrary());
        } else {
            qSpecLibraryPanel.setLibrary(configuration.getLibrary());
        }
    }

    @Override
    protected void applyEditorTo(@NotNull QSpecRunConfiguration configuration) {
        commonSettingsPanel.applyEditorTo(configuration);
        configuration.setExpectationPattern(expectationField.getText());
        configuration.setSpecificationPattern(specificationField.getText());

        final boolean inherit = inheritFromCheckBox.isSelected();
        configuration.setInheritLibrary(inherit);
        if (inherit) {
            configuration.setLibrary(null);
        } else {
            configuration.setLibrary(qSpecLibraryPanel.getLibrary());
        }
    }
}
