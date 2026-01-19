package org.kdb.inside.brains.view.editor;

import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.settings.KdbConfigurable;
import org.kdb.inside.brains.settings.KdbSettingsService;

import javax.swing.*;

public class EditorConfigurable extends KdbConfigurable {
    private final JBCheckBox rainbowBrace = new JBCheckBox("Rainbow paired brace");
    private final JBCheckBox rainbowParen = new JBCheckBox("Rainbow paired baren");
    private final JBCheckBox rainbowBracket = new JBCheckBox("Rainbow paired bracket");
    private final JBCheckBox rainbowVariables = new JBCheckBox("Rainbow variables");
    private final JBCheckBox highlightVector = new JBCheckBox("Highlight paired vector keys and values");

    private final KdbSettingsService settingsService = KdbSettingsService.getInstance();

    public EditorConfigurable() {
        super("Kdb.Settings.Editor", "Editor");
    }

    @Override
    public @Nullable JComponent createComponent() {
        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder
                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Rainbow"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(createRainbowPanel())

                .setFormLeftIndent(0)
                .addComponent(new TitledSeparator("Highlighting"))
                .setFormLeftIndent(FORM_LEFT_INDENT)
                .addComponent(createHighlightPanel())

                .addComponentFillVertically(new JPanel(), 0);

        return formBuilder.getPanel();
    }

    private JPanel createRainbowPanel() {
        final var formBuilder = FormBuilder.createFormBuilder();

        final String message = ApplicationBundle.message("rainbow.option.panel.display.name");

        final HyperlinkLabel label = new HyperlinkLabel();
        label.setTextWithHyperlink("Semantic highlighting must be enable for these options in <hyperlink>Editor > Color Schema > KDB+ Q > " + message + "</hyperlink>");
        label.addHyperlinkListener(e -> {
            final DataContext dataContext = DataManager.getInstance().getDataContext(rainbowBrace);
            ColorAndFontOptions.selectOrEditColor(dataContext,
                    message,
                    "KDB+ Q");
        });


        formBuilder.addComponent(label);
        formBuilder.addComponent(rainbowBrace);
        formBuilder.addComponent(rainbowParen);
        formBuilder.addComponent(rainbowBracket);
        formBuilder.addComponent(rainbowVariables);

        return formBuilder.getPanel();
    }

    private JPanel createHighlightPanel() {
        final var formBuilder = FormBuilder.createFormBuilder();

        formBuilder.addComponent(highlightVector);

        return formBuilder.getPanel();
    }

    @Override
    public boolean isModified() {
        return !option().equals(settingsService.getEditorOptions());
    }

    @Override
    public void apply() throws ConfigurationException {
        settingsService.setEditorOptions(option());
    }

    @Override
    public void reset() {
        final EditorOptions options = settingsService.getEditorOptions();

        rainbowBrace.setSelected(options.isRainbowBrace());
        rainbowParen.setSelected(options.isRainbowParen());
        rainbowBracket.setSelected(options.isRainbowBracket());
        rainbowVariables.setSelected(options.isRainbowVariables());
        highlightVector.setSelected(options.isHighlightVector());
    }

    private EditorOptions option() {
        final EditorOptions options = new EditorOptions();
        options.setRainbowBrace(rainbowBrace.isSelected());
        options.setRainbowParen(rainbowParen.isSelected());
        options.setRainbowBracket(rainbowBracket.isSelected());
        options.setRainbowVariables(rainbowVariables.isSelected());
        options.setHighlightVector(highlightVector.isSelected());
        return options;
    }
}