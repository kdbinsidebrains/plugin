package org.kdb.inside.brains.lang.formatting;

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

public class QCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
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
            spacing(consumer);
        } else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
            wrapping(consumer);
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

    private void spacing(@NotNull CodeStyleSettingsCustomizable consumer) {
        final String operators = "Operators";
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_ASSIGNMENT_OPERATORS", "Around assignment operators (::, :, ...)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ARITHMETIC", "Around arithmetic operators (+, -, * , %)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ORDER", "Around order operators (<= , >= , < , >)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_EQUALITY", "Around equality operators (~ , = , <>)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_WEIGHT", "Around weight operators (&, |)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_OTHERS", "Around mixed operators (!, #, @, _ , ? , ^, $)", operators);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AFTER_OPERATOR_COMMA", "After comma (,)", operators);

        // Lambda
        final String lambda = "Lambda ({..})";
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_WITHIN_BRACES", "Within braces", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_AFTER_PARAMETERS", "After parameters", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE", "Space before global close brace", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS", "Within parameter brackets", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON", "After parameter semicolon", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON", "Before parameter semicolon", lambda);

        // Control
        final String control = "Control statement (if, do, while, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_AFTER_OPERATOR", "After operator", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_WITHIN_BRACES", "Within brackets", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_AFTER_SEMICOLON", "After semicolon", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_BEFORE_SEMICOLON", "Before semicolon", control);

        // Condition
        final String condition = "Condition statement ($, @, ?, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_AFTER_OPERATOR", "After operator", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_WITHIN_BRACES", "Within brackets", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_AFTER_SEMICOLON", "After semicolon", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_SPACE_BEFORE_SEMICOLON", "Before semicolon", condition);

        // Arguments
        final String arguments = "Arguments (x[;;;])";
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_SPACE_WITHIN_BRACES", "Within brackets", arguments);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_SPACE_AFTER_SEMICOLON", "After semicolon", arguments);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_SPACE_BEFORE_SEMICOLON", "Before semicolon", arguments);

        // Arguments
        final String grouping = "Grouping ([.;.;.])";
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_SPACE_WITHIN_BRACES", "Within brackets", grouping);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_SPACE_AFTER_SEMICOLON", "After semicolon", grouping);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_SPACE_BEFORE_SEMICOLON", "Before semicolon", grouping);

        // Parentheses
        final String parentheses = "Parentheses ( (;;;))";
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_SPACE_WITHIN_PARENS", "Within parens", parentheses);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_SPACE_AFTER_SEMICOLON", "After semicolon", parentheses);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_SPACE_BEFORE_SEMICOLON", "Before semicolon", parentheses);

        // Mode
        final String mode = "Mode (k), M), ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "MODE_SPACE_AFTER", "After mode name", mode);

        // Commands
        final String commands = "Commands";
        consumer.showCustomOption(QCodeStyleSettings.class, "IMPORT_TRIM_TAIL", "Trim spaces after import command", commands);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTEXT_TRIM_TAIL", "Trim spaces after context command", commands);

        final String other = "Other";
        consumer.showCustomOption(QCodeStyleSettings.class, "RETURN_SPACE_AFTER_COLON", "After return colon", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "SIGNAL_SPACE_AFTER_SIGNAL", "After signal apostrophe", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_BEFORE_EXECUTION", "Before execution statement (.)", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "EXPRESSION_SEMICOLON_TRIM_SPACES", "Trim spaces before semicolon", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "EXPRESSION_SEMICOLON_REMOVE_LINES", "Remove bank lines before semicolon", other);
    }

    private void wrapping(@NotNull CodeStyleSettingsCustomizable consumer) {
        consumer.showStandardOptions(
                names(
                        WrappingOrBraceOption.RIGHT_MARGIN,
                        WrappingOrBraceOption.WRAP_ON_TYPING,
                        WrappingOrBraceOption.KEEP_LINE_BREAKS
                )
        );

        final String control = "Control statement (if, do, while, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_WRAP_TYPE", control, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_ALIGN_EXPRS", "Align when multiline", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_ALIGN_BRACKET", "Align brackets when multiline", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_LBRACKET_ON_NEXT_LINE", "New line after '['", control);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", control);

        final String condition = "Condition statement (?[], $[], @[], .[], ![], ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_WRAP_TYPE", condition, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_ALIGN_EXPRS", "Align when multiline", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_ALIGN_BRACKET", "Align brackets when multiline", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_LBRACKET_ON_NEXT_LINE", "New line after '['", condition);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", condition);

        final String arguments = "Arguments (x[...])";
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_WRAP", arguments, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_ALIGN_EXPRS", "Align when multiline", arguments);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_ALIGN_BRACKET", "Align brackets when multiline", arguments);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_LBRACKET_ON_NEXT_LINE", "New line after '['", arguments);
        consumer.showCustomOption(QCodeStyleSettings.class, "ARGUMENTS_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", arguments);

        final String grouping = "Grouping ([.;.;.])";
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_WRAP", grouping, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_ALIGN_EXPRS", "Align when multiline", grouping);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_ALIGN_BRACKET", "Align brackets when multiline", grouping);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_LBRACKET_ON_NEXT_LINE", "New line after '['", grouping);
        consumer.showCustomOption(QCodeStyleSettings.class, "GROUPING_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", grouping);

        final String parentheses = "Parentheses ( (;;;))";
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_WRAP", parentheses, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_ALIGN_EXPRS", "Align when multiline", parentheses);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_ALIGN_PAREN", "Align parens when multiline", parentheses);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_LPAREN_ON_NEXT_LINE", "New line after '('", parentheses);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARENTHESES_RPAREN_ON_NEXT_LINE", "Place ')' on new line", parentheses);

        final String lambda = "Lambda ({..})";
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_ALIGN_BRACE", "Align braces", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_PARAMS_WRAP", "Wrap parameters", lambda, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_PARAMS_ALIGN_NAMES", "Align parameters when multiline", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_PARAMS_ALIGN_BRACKETS", "Align parameter brackets when multiline", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_PARAMS_LBRACKET_ON_NEXT_LINE", "New line after '['", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_PARAMS_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", lambda);

        final String mode = "Mode (k), M), ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "MODE_WRAP_TYPE", mode, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "MODE_ALIGN", "Align when multiline", mode);
    }

    @Override
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new QCodeStylePanel(getCurrentSettings(), settings);
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
