package org.kdb.inside.brains.lang.codestyle;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.*;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.SpacingOption;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.WrappingOrBraceOption;

public class QLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @Override
    public @NotNull Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @Nullable String getCodeSample(@NotNull SettingsType settingsType) {
        InputStream s = getClass().getResourceAsStream("/org/kdb/inside/brains/codeStyle/" + settingsType.name().toLowerCase() + ".txt");
        if (s == null) {
            s = getClass().getResourceAsStream("/org/kdb/inside/brains/codeStyle/default.txt");
        }

        if (s == null) {
            return null;
        }

        try {
            return IOUtils.toString(s, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            customizeSpacing(consumer);
        } else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
            customizeWrapping(consumer);
/*
        } else if (settingsType == SettingsType.BLANK_LINES_SETTINGS) {
            consumer.showAllStandardOptions();
*/
        }
    }

    @Override
    public CustomCodeStyleSettings createCustomSettings(CodeStyleSettings settings) {
        return new QCodeStyleSettings(settings);
    }

    @Override
    protected void customizeDefaults(@NotNull CommonCodeStyleSettings commonSettings, CommonCodeStyleSettings.@NotNull IndentOptions indentOptions) {
        commonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS = false;
    }

    private void customizeWrapping(@NotNull CodeStyleSettingsCustomizable consumer) {
        consumer.showStandardOptions(
                names(
                        WrappingOrBraceOption.RIGHT_MARGIN,
                        WrappingOrBraceOption.WRAP_ON_TYPING,
                        WrappingOrBraceOption.KEEP_LINE_BREAKS,
                        WrappingOrBraceOption.METHOD_PARAMETERS_WRAP,
                        WrappingOrBraceOption.ALIGN_MULTILINE_PARAMETERS,
                        WrappingOrBraceOption.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE,
                        WrappingOrBraceOption.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
                )
        );

        consumer.renameStandardOption(WrappingOrBraceOption.METHOD_PARAMETERS_WRAP.name(),
                "Lambda declaration parameters");
        consumer.renameStandardOption(WrappingOrBraceOption.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE.name(),
                "New line after '['");
        consumer.renameStandardOption(WrappingOrBraceOption.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE.name(),
                "Place ']' on new line");

/*
        consumer.showCustomOption(QCodeStyleSettings.class,
                "ASDAS_ASDASDADQWQWE_ASDASDASD",
                "Is iasdf sdf asdfew asdf asdf",
                "Lambda declaration parameters",
                CodeStyleSettingsCustomizableOptions.getInstance().WRAP_OPTIONS, WRAP_VALUES);
*/

/*
            consumer.showCustomOption(JsonCodeStyleSettings.class,
                    "KEEP_TRAILING_COMMA",
                    JsonBundle.message("formatter.trailing_comma.label"),
                    getInstance().WRAPPING_KEEP);

            consumer.showCustomOption(JsonCodeStyleSettings.class,
                    "ARRAY_WRAPPING",
                    JsonBundle.message("formatter.wrapping_arrays.label"),
                    null,
                    getInstance().WRAP_OPTIONS,
                    CodeStyleSettingsCustomizable.WRAP_VALUES);

            consumer.showCustomOption(JsonCodeStyleSettings.class,
                    "OBJECT_WRAPPING",
                    JsonBundle.message("formatter.objects.label"),
                    null,
                    getInstance().WRAP_OPTIONS,
                    CodeStyleSettingsCustomizable.WRAP_VALUES);*/
/*
            consumer.showCustomOption(JsonCodeStyleSettings.class,
                    "PROPERTY_ALIGNMENT",
                    JsonBundle.message("formatter.align.properties.caption"),
                    JsonBundle.message("formatter.objects.label"),
                    JsonLanguageCodeStyleSettingsProvider.Holder.ALIGN_OPTIONS,
                    JsonLanguageCodeStyleSettingsProvider.Holder.ALIGN_VALUES);*/
    }

    private void customizeSpacing(@NotNull CodeStyleSettingsCustomizable consumer) {
        consumer.showStandardOptions(
                names(
                        SpacingOption.SPACE_AROUND_ASSIGNMENT_OPERATORS
                )
        );

        consumer.renameStandardOption(name(SpacingOption.SPACE_AROUND_ASSIGNMENT_OPERATORS), "Assignment operators (::, :, ...)");

        final String lambda_definition = "Lambda definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_BEFORE_BRACE_CLOSE", "Before close brace", lambda_definition);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AFTER_LAMBDA_PARAMETERS", "After parameter semicolon", lambda_definition);
    }

    @Override
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new QCodeStyleMainPanel(getCurrentSettings(), settings);
            }
        };
    }

    private String name(Enum<?> en) {
        return en.name();
    }

    private String[] names(Enum<?>... enums) {
        return Stream.of(enums).map(Enum::name).toArray(String[]::new);
    }
}
