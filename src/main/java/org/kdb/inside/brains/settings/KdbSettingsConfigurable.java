package org.kdb.inside.brains.settings;

import com.intellij.openapi.util.Comparing;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.ExecutionOptionsPanel;
import org.kdb.inside.brains.view.console.ConsoleOptionsPanel;
import org.kdb.inside.brains.view.console.NumericalOptionsPanel;
import org.kdb.inside.brains.view.console.TableOptionsPanel;
import org.kdb.inside.brains.view.inspector.InspectorOptionsPanel;

import javax.swing.*;

public class KdbSettingsConfigurable extends KdbConfigurable {
    private final TableOptionsPanel tableOptionsPanel = new TableOptionsPanel();
    private final NumericalOptionsPanel numericalOptionsPanel = new NumericalOptionsPanel();
    private final ConsoleOptionsPanel consoleOptionsPanel = new ConsoleOptionsPanel();
    private final ExecutionOptionsPanel executionOptionsPanel = new ExecutionOptionsPanel();
    private final InspectorOptionsPanel inspectorOptionsPanel = new InspectorOptionsPanel();

    private final KdbSettingsService settingsService = KdbSettingsService.getInstance();

    public KdbSettingsConfigurable() {
        super("Kdb.Settings", "KDB+ Q Settings");
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder
                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Console Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(consoleOptionsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Numerical Format"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(numericalOptionsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Table Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(tableOptionsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Execution Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(executionOptionsPanel)

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Inspector Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(inspectorOptionsPanel)

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    @Override
    public boolean isModified() {
        if (!Comparing.equal(settingsService.getConsoleOptions(), consoleOptionsPanel.getOptions())) {
            return true;
        }

        if (!Comparing.equal(settingsService.getNumericalOptions(), numericalOptionsPanel.getOptions())) {
            return true;
        }

        if (!Comparing.equal(settingsService.getTableOptions(), tableOptionsPanel.getOptions())) {
            return true;
        }

        if (!Comparing.equal(settingsService.getExecutionOptions(), executionOptionsPanel.getOptions())) {
            return true;
        }

        if (!Comparing.equal(settingsService.getInspectorOptions(), inspectorOptionsPanel.getOptions())) {
            return true;
        }
        return false;
    }

    @Override
    public void apply() {
        settingsService.setTableOptions(tableOptionsPanel.getOptions());
        settingsService.setConsoleOptions(consoleOptionsPanel.getOptions());
        settingsService.setExecutionOptions(executionOptionsPanel.getOptions());
        settingsService.setInspectorOptions(inspectorOptionsPanel.getOptions());
        settingsService.setNumericalOptions(numericalOptionsPanel.getOptions());
    }

    @Override
    public void reset() {
        tableOptionsPanel.setOptions(settingsService.getTableOptions());
        consoleOptionsPanel.setOptions(settingsService.getConsoleOptions());
        executionOptionsPanel.setOptions(settingsService.getExecutionOptions());
        inspectorOptionsPanel.setOptions(settingsService.getInspectorOptions());
        numericalOptionsPanel.setOptions(settingsService.getNumericalOptions());
    }
}
