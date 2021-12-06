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

import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable.WrappingOrBraceOption;
import static com.intellij.psi.codeStyle.CodeStyleSettingsCustomizableOptions.getInstance;

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

        final String controlWrapTitle = "Control statement (if, do, while, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_WRAP_TYPE", controlWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_WRAP_ALIGN", "Align when multiline", controlWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_OBRACKET_ON_NEXT_LINE", "New line after '['", controlWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_CBRACKET_ON_NEXT_LINE", "Place ']' on new line", controlWrapTitle);

        final String conditionWrapTitle = "Condition statement (?[], $[], @[], .[], ![], ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_WRAP_TYPE", conditionWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_WRAP_ALIGN", "Align when multiline", conditionWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_OBRACKET_ON_NEXT_LINE", "New line after '['", conditionWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_CBRACKET_ON_NEXT_LINE", "Place ']' on new line", conditionWrapTitle);

        final String tableWrapTitle = "Table definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_WRAP_TYPE", tableWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_WRAP_ALIGN", "Align when multiline", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_KEYS_EMPTY_LINE", "Place '[' and ']' on new lines, if wrapped", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CPAREN_EMPTY_LINE", "Place ')' on new line, if wrapped", tableWrapTitle);

//        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_KEYS_EMPTY_LINE", "Place keys bracket on new line", tableWrapTitle);
/*
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_OPAREN_ON_NEXT_LINE", "New line after '('", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CPAREN_ON_NEXT_LINE", "Place ')' on new line", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_OBRACKET_ON_NEXT_LINE", "New line after '[', if not empty", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CBRACKET_ON_NEXT_LINE", "Place ']' on new line, if not empty", tableWrapTitle);
*/

        // Expressions
        final String semicolonSpaces = "Expressions";
        consumer.showCustomOption(QCodeStyleSettings.class, "EXPRESSION_SEMICOLON_ON_NEW_LINE", "Allow semicolon on new line", semicolonSpaces);
    }

    private void customizeSpacing(@NotNull CodeStyleSettingsCustomizable consumer) {
        final String operatorSpaces = "Around operators";
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_ASSIGNMENT_OPERATORS", "Assignment operators (::, :, ...)", operatorSpaces);

        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ARITHMETIC", "Arithmetic operators (+, -, * , %)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ORDER", "Order operators (<= , >= , < , >)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_EQUALITY", "Equality operators (~ , = , <>)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_WEIGHT", "Weight operators (&, |)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_OTHERS", "Mixed operators (!, #, @, _ , ? , ., ^, $)", operatorSpaces);

        // Lambda settings
        final String lambdaSection = "Lambda definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_BEFORE_BRACE_CLOSE", "Before close brace", lambdaSection);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_AFTER_PARAMETERS", "After parameter semicolon", lambdaSection);

        // Tables
        final String tableSpaces = "Table definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_SPACE_AFTER_KEY_COLUMNS", "After keys group", tableSpaces);
//        consumer.showCustomOption(QCodeStyleSettings.class, "CONTEXT_TRIM_TAIL", "Trim spaces after context command", tableSpaces);

        // Control settings
        final String controlSection = "Control statement (if, do, while, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_AFTER_OPERATOR", "After operator", controlSection);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_WITHIN_BRACES", "Within braces", controlSection);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_AFTER_SEMICOLON", "After semicolon", controlSection);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_BEFORE_SEMICOLON", "Before semicolon", controlSection);

        // Condition settings
        final String conditionStatement = "Condition statement ($, @, ?, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_AFTER_OPERATOR", "After operator", conditionStatement);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_WITHIN_BRACES", "Within braces", conditionStatement);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_AFTER_SEMICOLON", "After semicolon", conditionStatement);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_BEFORE_SEMICOLON", "Before semicolon", conditionStatement);

        // Commands
        final String tailSpaces = "Commands";
        consumer.showCustomOption(QCodeStyleSettings.class, "IMPORT_TRIM_TAIL", "Trim spaces after import command", tailSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTEXT_TRIM_TAIL", "Trim spaces after context command", tailSpaces);
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
