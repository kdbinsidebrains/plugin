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
                        WrappingOrBraceOption.KEEP_LINE_BREAKS
                )
        );

        final String controlWrapTitle = "Control statement (if, do, while, ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_WRAP_TYPE", controlWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_ALIGN_EXPRS", "Align when multiline", controlWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_ALIGN_BRACKET", "Align bracket when multiline", controlWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_LBRACKET_ON_NEXT_LINE", "New line after '['", controlWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", controlWrapTitle);

        final String conditionWrapTitle = "Condition statement (?[], $[], @[], .[], ![], ...)";
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_WRAP_TYPE", conditionWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_ALIGN_EXPRS", "Align when multiline", conditionWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_ALIGN_BRACKET", "Align bracket when multiline", conditionWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_LBRACKET_ON_NEXT_LINE", "New line after '['", conditionWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONDITION_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", conditionWrapTitle);

        final String lambda = "Lambda definition ({[] }})";
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_ALIGN_BRACE", "Align braces", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE", "Space before global close brace", lambda);

        final String parameters = "Lambda parameters ([x;y;x;...])";
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_WRAP", parameters, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_ALIGN_VARS", "Align when multiline", parameters);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_ALIGN_BRACKET", "Align bracket when multiline", parameters);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_LBRACKET_ON_NEXT_LINE", "New line after '['", parameters);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_RBRACKET_ON_NEXT_LINE", "Place ']' on new line", parameters);

/*
        final String tableWrapTitle = "Table definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_WRAP_TYPE", tableWrapTitle, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_WRAP_ALIGN", "Align when multiline", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_KEYS_EMPTY_LINE", "Place '[' and ']' on new lines, if wrapped", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CPAREN_EMPTY_LINE", "Place ')' on new line, if wrapped", tableWrapTitle);
        */

//        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_KEYS_EMPTY_LINE", "Place keys bracket on new line", tableWrapTitle);
/*
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_OPAREN_ON_NEXT_LINE", "New line after '('", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CPAREN_ON_NEXT_LINE", "Place ')' on new line", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_OBRACKET_ON_NEXT_LINE", "New line after '[', if not empty", tableWrapTitle);
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_CBRACKET_ON_NEXT_LINE", "Place ']' on new line, if not empty", tableWrapTitle);
*/

        // Expressions
//        final String semicolonSpaces = "Ending semicolon";
    }

    private void customizeSpacing(@NotNull CodeStyleSettingsCustomizable consumer) {
        final String operatorSpaces = "Around operators";
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_ASSIGNMENT_OPERATORS", "Assignment operators (::, :, ...)", operatorSpaces);

        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ARITHMETIC", "Arithmetic operators (+, -, * , %)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_ORDER", "Order operators (<= , >= , < , >)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_EQUALITY", "Equality operators (~ , = , <>)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_WEIGHT", "Weight operators (&, |)", operatorSpaces);
        consumer.showCustomOption(QCodeStyleSettings.class, "SPACE_AROUND_OPERATOR_OTHERS", "Mixed operators (!, #, @, _ , ? , ^, $)", operatorSpaces);

        // Lambda definition
        final String lambda = "Lambda definition ({[] }})";
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_AFTER_PARAMETERS", "Space after parameters", lambda);
        consumer.showCustomOption(QCodeStyleSettings.class, "LAMBDA_SPACE_BEFORE_CLOSE_BRACE", "Space before close brace", lambda);

        // Lambda parameters
        final String parameters = "Lambda parameters";
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_SPACE_WITHIN_BRACKET", "Within parameter braces", parameters);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_SPACE_AFTER_SEMICOLON", "After parameter semicolon", parameters);
        consumer.showCustomOption(QCodeStyleSettings.class, "PARAMS_SPACE_BEFORE_SEMICOLON", "Before parameter semicolon", parameters);

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

        final String other = "Other";
        consumer.showCustomOption(QCodeStyleSettings.class, "RETURN_SPACE_AFTER_COLON", "After return colon", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "SIGNAL_SPACE_AFTER_SIGNAL", "After signal apostrophe", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "CONTROL_SPACE_BEFORE_EXECUTION", "Before execution statement (.)", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "EXPRESSION_SEMICOLON_TRIM_SPACES", "Trim spaces before semicolon", other);
        consumer.showCustomOption(QCodeStyleSettings.class, "EXPRESSION_SEMICOLON_REMOVE_LINES", "Remove bank lines before semicolon", other);


/*
        // Tables
        final String tableSpaces = "Table definition";
        consumer.showCustomOption(QCodeStyleSettings.class, "TABLE_SPACE_AFTER_KEY_COLUMNS", "After keys group", tableSpaces);
//        consumer.showCustomOption(QCodeStyleSettings.class, "CONTEXT_TRIM_TAIL", "Trim spaces after context command", tableSpaces);
*/

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
