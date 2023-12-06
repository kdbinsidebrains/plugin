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
    private static final String OPERATORS = "Operators";
    private static final String ITERATORS = "Iterators";
    private static final String LAMBDA = "Lambda ({..})";
    private static final String TABLE = "Table definition (([...]...))";
    private static final String CONTROL = "Control statement (if, do, while, ...)";
    private static final String CONDITION = "Condition statement ($, @, ?, ...)";
    private static final String ARGUMENTS = "Arguments (x[;;;])";
    private static final String GROUPING = "Grouping ([.;.;.])";
    private static final String PARENTHESES = "Parentheses ( (;;;))";
    private static final String EXECUTION = "Execution (f`p, f `p, f[`p])";
    private static final String MODE = "Mode (k), M), ...)";
    private static final String COMMANDS = "Commands";
    private static final String QUERY = "Query (select by from)";
    private static final String OTHER = "Other";

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
        final Customizer customizer = new Customizer(consumer);
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            spacing(customizer);
        } else if (settingsType == SettingsType.WRAPPING_AND_BRACES_SETTINGS) {
            wrapping(customizer);
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
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings, @NotNull CodeStyleSettings modelSettings) {
        return new CodeStyleAbstractConfigurable(settings, modelSettings, this.getConfigurableDisplayName()) {
            @Override
            protected CodeStyleAbstractPanel createPanel(CodeStyleSettings settings) {
                return new QCodeStylePanel(getCurrentSettings(), settings);
            }
        };
    }

    private void spacing(Customizer customizer) {
        final Group operators = customizer.group(OPERATORS);
        operators.item("SPACE_AROUND_ASSIGNMENT_OPERATORS", "Around assignment operators (::, :, ...)");
        operators.item("SPACE_AROUND_OPERATOR_ARITHMETIC", "Around arithmetic operators (+, -, * , %)");
        operators.item("SPACE_AROUND_OPERATOR_ORDER", "Around order operators (<= , >= , < , >)");
        operators.item("SPACE_AROUND_OPERATOR_EQUALITY", "Around equality operators (~ , = , <>)");
        operators.item("SPACE_AROUND_OPERATOR_WEIGHT", "Around weight operators (&, |)");
        operators.item("SPACE_AROUND_OPERATOR_OTHERS", "Around mixed operators (!, #, @, ? , ^, $)");
        operators.item("SPACE_AROUND_OPERATOR_CUT", "Around cut but vars and symbols ( _ )");
        operators.item("SPACE_AFTER_OPERATOR_COMMA", "After comma (,)");

        final Group iterators = customizer.group(ITERATORS);
        iterators.item("ITERATOR_SPACE_AROUND", "Around iterators but operator");
        iterators.item("ITERATOR_SPACE_BETWEEN", "Between iterators");
        iterators.item("ITERATOR_SPACE_AFTER_OPERATOR", "Before operator");

        final Group lambda = customizer.group(LAMBDA);
        lambda.item("LAMBDA_SPACE_WITHIN_BRACES", "Within braces");
        lambda.item("LAMBDA_SPACE_AFTER_PARAMETERS", "After parameters");
        lambda.item("LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS", "Within parameter brackets");
        lambda.item("LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON", "After parameter semicolon");
        lambda.item("LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON", "Before parameter semicolon");
        lambda.item("LAMBDA_GLOBAL_SPACE_BEFORE_CLOSE_BRACE", "Space before global close brace");

        final Group table = customizer.group(TABLE);
//        table.item("TABLE_SPACE_WITHIN_PARENS", "Within parens");
        table.item("TABLE_SPACE_AFTER_KEY_SEMICOLON", "After key semicolon");
        table.item("TABLE_SPACE_BEFORE_KEY_SEMICOLON", "Before key semicolon");
        table.item("TABLE_SPACE_AFTER_COLUMN_SEMICOLON", "After column semicolon");
        table.item("TABLE_SPACE_BEFORE_COLUMN_SEMICOLON", "Before column semicolon");
        table.item("TABLE_SPACE_BEFORE_COLUMNS", "Before columns");
        table.item("TABLE_SPACE_AFTER_COLUMNS", "After columns");
        table.item("TABLE_SPACE_BEFORE_GLOBAL_CLOSE_BRACKET", "Space before global close brace");

        final Group control = customizer.group(CONTROL);
        control.item("CONTROL_SPACE_AFTER_OPERATOR", "After operator");
        control.item("CONTROL_SPACE_WITHIN_BRACES", "Within brackets");
        control.item("CONTROL_SPACE_AFTER_SEMICOLON", "After semicolon");
        control.item("CONTROL_SPACE_BEFORE_SEMICOLON", "Before semicolon");

        final Group condition = customizer.group(CONDITION);
        condition.item("CONDITION_SPACE_AFTER_OPERATOR", "After operator");
        condition.item("CONDITION_SPACE_WITHIN_BRACES", "Within brackets");
        condition.item("CONDITION_SPACE_AFTER_SEMICOLON", "After semicolon");
        condition.item("CONDITION_SPACE_BEFORE_SEMICOLON", "Before semicolon");

        final Group execution = customizer.group(EXECUTION);
        execution.item("EXECUTION_SPACE_BEFORE_SYMBOLS", "Before symbol(s)");
        execution.item("EXECUTION_SPACE_BEFORE_ARGUMENTS", "Before arguments");
        execution.item("EXECUTION_SPACE_BEFORE_PARAMETER", "Before parameter");
        execution.item("EXECUTION_SPACE_AFTER_INTERNAL", "After internal function (-[0-9]+!)");

        final Group arguments = customizer.group(ARGUMENTS);
        arguments.item("ARGUMENTS_SPACE_WITHIN_BRACES", "Within brackets");
        arguments.item("ARGUMENTS_SPACE_AFTER_SEMICOLON", "After semicolon");
        arguments.item("ARGUMENTS_SPACE_BEFORE_SEMICOLON", "Before semicolon");

        final Group grouping = customizer.group(GROUPING);
        grouping.item("GROUPING_SPACE_WITHIN_BRACES", "Within brackets");
        grouping.item("GROUPING_SPACE_AFTER_SEMICOLON", "After semicolon");
        grouping.item("GROUPING_SPACE_BEFORE_SEMICOLON", "Before semicolon");

        final Group parentheses = customizer.group(PARENTHESES);
        parentheses.item("PARENTHESES_SPACE_WITHIN_PARENS", "Within parens");
        parentheses.item("PARENTHESES_SPACE_AFTER_SEMICOLON", "After semicolon");
        parentheses.item("PARENTHESES_SPACE_BEFORE_SEMICOLON", "Before semicolon");

        final Group mode = customizer.group(MODE);
        mode.item("MODE_SPACE_AFTER", "After mode name");

        final Group commands = customizer.group(COMMANDS);
        commands.item("IMPORT_TRIM_TAIL", "Trim spaces after import command");
        commands.item("CONTEXT_TRIM_TAIL", "Trim spaces after context command");

        final Group query = customizer.group(QUERY);
        query.item("QUERY_SPACE_AFTER_COMMA", "After column's comma");

        final Group others = customizer.group(OTHER);
        others.item("RETURN_SPACE_AFTER_COLON", "After return colon");
        others.item("SIGNAL_SPACE_AFTER_SIGNAL", "After signal apostrophe");
        others.item("FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL", "Before symbol(s) in an invoke");
        others.item("SEMICOLON_SPACE_AFTER", "After expression's semicolon");
        others.item("EXPRESSION_SEMICOLON_TRIM_SPACES", "Trim spaces before semicolon");
        others.item("LINE_COMMENT_TRIM_SPACES", "Trim spaces before inline comment");
        others.item("EXPRESSION_SEMICOLON_REMOVE_LINES", "Remove bank lines before semicolon");
    }

    private void wrapping(@NotNull Customizer customizer) {
        customizer.consumer.showStandardOptions(
                names(
                        WrappingOrBraceOption.RIGHT_MARGIN,
                        WrappingOrBraceOption.WRAP_ON_TYPING,
                        WrappingOrBraceOption.KEEP_LINE_BREAKS
                )
        );

        final Group control = customizer.wrap(CONTROL, "CONTROL_WRAP_TYPE");
        control.item("CONTROL_ALIGN_EXPRS", "Align when multiline");
        control.item("CONTROL_ALIGN_BRACKET", "Align brackets when multiline");
        control.item("CONTROL_LBRACKET_ON_NEXT_LINE", "New line after '['");
        control.item("CONTROL_RBRACKET_ON_NEXT_LINE", "Place ']' on new line");

        final Group condition = customizer.wrap(CONDITION, "CONDITION_WRAP_TYPE");
        condition.item("CONDITION_ALIGN_EXPRS", "Align when multiline");
        condition.item("CONDITION_ALIGN_BRACKET", "Align brackets when multiline");
        condition.item("CONDITION_LBRACKET_ON_NEXT_LINE", "New line after '['");
        condition.item("CONDITION_RBRACKET_ON_NEXT_LINE", "Place ']' on new line");

        final Group arguments = customizer.wrap(ARGUMENTS, "ARGUMENTS_WRAP");
        arguments.item("ARGUMENTS_ALIGN_EXPRS", "Align when multiline");
        arguments.item("ARGUMENTS_ALIGN_BRACKET", "Align brackets when multiline");
        arguments.item("ARGUMENTS_LBRACKET_ON_NEXT_LINE", "New line after '['");
        arguments.item("ARGUMENTS_RBRACKET_ON_NEXT_LINE", "Place ']' on new line");

        final Group grouping = customizer.wrap(GROUPING, "GROUPING_WRAP");
        grouping.item("GROUPING_ALIGN_EXPRS", "Align when multiline");
        grouping.item("GROUPING_ALIGN_BRACKET", "Align brackets when multiline");
        grouping.item("GROUPING_LBRACKET_ON_NEXT_LINE", "New line after '['");
        grouping.item("GROUPING_RBRACKET_ON_NEXT_LINE", "Place ']' on new line");

        final Group parentheses = customizer.wrap(PARENTHESES, "PARENTHESES_WRAP");
        parentheses.item("PARENTHESES_ALIGN_EXPRS", "Align when multiline");
        parentheses.item("PARENTHESES_ALIGN_PAREN", "Align parens when multiline");
        parentheses.item("PARENTHESES_LPAREN_ON_NEXT_LINE", "New line after '('");
        parentheses.item("PARENTHESES_RPAREN_ON_NEXT_LINE", "Place ')' on new line");

        final Group lambda = customizer.group(LAMBDA);
        lambda.item("LAMBDA_ALIGN_BRACE", "Align braces");
        lambda.wrap("LAMBDA_PARAMS_WRAP", "Wrap parameters");
        lambda.item("LAMBDA_PARAMS_ALIGN_NAMES", "Align parameters when multiline");
        lambda.item("LAMBDA_PARAMS_ALIGN_BRACKETS", "Align parameter brackets when multiline");
        lambda.item("LAMBDA_PARAMS_LBRACKET_ON_NEXT_LINE", "New line after '['");
        lambda.item("LAMBDA_PARAMS_RBRACKET_ON_NEXT_LINE", "Place ']' on new line");

        final Group table = customizer.group(TABLE);
        table.wrap("TABLE_COLUMNS_WRAP", "Wrap columns");
        table.item("TABLE_ALIGN_COLUMNS", "Align columns when multiline");
        table.item("TABLE_LBRACKET_NEW_LINE", "Place '[' on new line");
        table.item("TABLE_RBRACKET_NEW_LINE", "Place ']' on new line");
        table.item("TABLE_ALIGN_BRACKETS", "Align brackets when multiline");
        table.item("TABLE_CLOSE_PAREN_NEW_LINE", "Place ')' on new line");
        table.item("TABLE_ALIGN_PARENS", "Align parens when multiline");

        final Group query = customizer.group(QUERY);
        query.wrap("QUERY_WRAP_PARTS", "Wrap parts");
        query.item("QUERY_PARTS_ALIGN", "Align parts by right");
        query.wrap("QUERY_WRAP_COLUMNS", "Wrap columns");
        query.item("QUERY_COLUMNS_ALIGN", "Align columns when multiline");

        final Group mode = customizer.wrap(MODE, "MODE_WRAP_TYPE");
        mode.item("MODE_ALIGN", "Align when multiline");

        final Group others = customizer.group(OTHER);
        others.item("INVOKE_ALIGN_ITEMS", "Align invoke expressions");
    }

    private String[] names(Enum<?>... enums) {
        return Stream.of(enums).map(Enum::name).toArray(String[]::new);
    }

    private static class Customizer {
        private final CodeStyleSettingsCustomizable consumer;

        public Customizer(CodeStyleSettingsCustomizable consumer) {
            this.consumer = consumer;
        }

        Group group(String name) {
            return new Group(name, consumer);
        }

        public Group wrap(String group, String field) {
            consumer.showCustomOption(QCodeStyleSettings.class, field, group, null, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
            return new Group(group, consumer);
        }
    }

    private static class Group {
        private final String group;
        private final CodeStyleSettingsCustomizable consumer;

        public Group(String group, CodeStyleSettingsCustomizable consumer) {
            this.group = group;
            this.consumer = consumer;
        }

        void item(@NotNull String fieldName, @NotNull String title) {
            consumer.showCustomOption(QCodeStyleSettings.class, fieldName, title, group);
        }

        void wrap(@NotNull String fieldName, @NotNull String title) {
            consumer.showCustomOption(QCodeStyleSettings.class, fieldName, title, group, getInstance().WRAP_OPTIONS, CodeStyleSettingsCustomizable.WRAP_VALUES);
        }
    }
}
