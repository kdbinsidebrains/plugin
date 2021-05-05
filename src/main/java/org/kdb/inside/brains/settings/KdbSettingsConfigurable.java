package org.kdb.inside.brains.settings;

import com.intellij.openapi.util.Comparing;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.ExecutionOptionsPanel;
import org.kdb.inside.brains.view.console.ConsoleOptionsPanel;

import javax.swing.*;

public class KdbSettingsConfigurable extends KdbConfigurable {
    private final ConsoleOptionsPanel consoleOptionsPanel = new ConsoleOptionsPanel();
    private final ExecutionOptionsPanel executionOptionsPanel = new ExecutionOptionsPanel();

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
                .addComponent(new TitledSeparator("Execution Options"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(executionOptionsPanel)

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    @Override
    public boolean isModified() {
        if (!Comparing.equal(settingsService.getConsoleOptions(), consoleOptionsPanel.getConsoleOptions())) {
            return true;
        }

        return !Comparing.equal(settingsService.getConnectionOptions(), executionOptionsPanel.getOptions());
    }

    @Override
    public void apply() {
        settingsService.setConsoleOptions(consoleOptionsPanel.getConsoleOptions());
        settingsService.setConnectionOptions(executionOptionsPanel.getOptions());
    }

    @Override
    public void reset() {
        consoleOptionsPanel.setConsoleOptions(settingsService.getConsoleOptions());
        executionOptionsPanel.setOptions(settingsService.getConnectionOptions());
    }
}
