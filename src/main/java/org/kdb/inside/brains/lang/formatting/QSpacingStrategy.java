package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;

import java.util.List;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QSpacingStrategy {
    private final SpacingBuilder builder;

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        final var custom = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);
        final var common = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Lambda
        builder.afterInside(SEMICOLON, PARAMETERS).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON);
        builder.beforeInside(SEMICOLON, PARAMETERS).spaceIf(custom.LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON);
        builder.afterInside(BRACKET_OPEN, PARAMETERS).spaceIf(custom.LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS, custom.LAMBDA_PARAMS_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(BRACKET_CLOSE, PARAMETERS).spaceIf(custom.LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS, custom.LAMBDA_PARAMS_RBRACKET_ON_NEXT_LINE);
        builder.betweenInside(PARAMETERS, EXPRESSIONS, LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMETERS);
        builder.betweenInside(BRACE_OPEN, EXPRESSIONS, LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_WITHIN_BRACES);
        builder.betweenInside(EXPRESSIONS, BRACE_CLOSE, LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_WITHIN_BRACES);

        // Table
        builder.afterInside(SEMICOLON, TABLE_KEYS).spaceIf(custom.TABLE_SPACE_AFTER_KEY_SEMICOLON);
        builder.beforeInside(SEMICOLON, TABLE_KEYS).spaceIf(custom.TABLE_SPACE_BEFORE_KEY_SEMICOLON);
        // See TableBlock#getSpaces for custom rule as this one doesn't cover it
        builder.afterInside(BRACKET_OPEN, TABLE_KEYS).spaceIf(false, custom.TABLE_LBRACKET_NEW_LINE);
        builder.beforeInside(BRACKET_CLOSE, TABLE_KEYS).spaceIf(false, custom.TABLE_RBRACKET_NEW_LINE);
        builder.beforeInside(TABLE_VALUES, TABLE_EXPR).spaceIf(custom.TABLE_SPACE_BEFORE_COLUMNS, custom.TABLE_RBRACKET_NEW_LINE);
        builder.beforeInside(PAREN_CLOSE, TABLE_EXPR).spaceIf(custom.TABLE_SPACE_AFTER_COLUMNS, custom.TABLE_CLOSE_PAREN_NEW_LINE);
        builder.afterInside(SEMICOLON, TABLE_VALUES).spaceIf(custom.TABLE_SPACE_AFTER_COLUMN_SEMICOLON);
        builder.beforeInside(SEMICOLON, TABLE_VALUES).spaceIf(custom.TABLE_SPACE_BEFORE_COLUMN_SEMICOLON);
        builder.beforeInside(TABLE_KEYS, TABLE_EXPR).spaces(0);

        // Control
        builder.after(CONTROL_KEYWORD).spaceIf(custom.CONTROL_SPACE_AFTER_OPERATOR);
        builder.afterInside(BRACKET_OPEN, CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_WITHIN_BRACES, custom.CONTROL_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(BRACKET_CLOSE, CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_WITHIN_BRACES, custom.CONTROL_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(SEMICOLON, CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(SEMICOLON, CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_BEFORE_SEMICOLON);

        // Condition
        builder.after(CONDITION_KEYWORD).spaceIf(custom.CONDITION_SPACE_AFTER_OPERATOR);
        builder.afterInside(BRACKET_OPEN, CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_WITHIN_BRACES, custom.CONDITION_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(BRACKET_CLOSE, CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_WITHIN_BRACES, custom.CONDITION_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(SEMICOLON, CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(SEMICOLON, CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_BEFORE_SEMICOLON);

        // Arguments
        builder.afterInside(BRACKET_OPEN, ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_WITHIN_BRACES, custom.ARGUMENTS_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(BRACKET_CLOSE, ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_WITHIN_BRACES, custom.ARGUMENTS_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(SEMICOLON, ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(SEMICOLON, ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_BEFORE_SEMICOLON);

        // Grouping
        builder.afterInside(BRACKET_OPEN, GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_WITHIN_BRACES, custom.GROUPING_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(BRACKET_CLOSE, GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_WITHIN_BRACES, custom.GROUPING_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(SEMICOLON, GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(SEMICOLON, GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_BEFORE_SEMICOLON);

        // Parentheses
        builder.afterInside(PAREN_OPEN, PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_WITHIN_PARENS, custom.PARENTHESES_LPAREN_ON_NEXT_LINE);
        builder.beforeInside(PAREN_CLOSE, PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_WITHIN_PARENS, custom.PARENTHESES_RPAREN_ON_NEXT_LINE);
        builder.afterInside(SEMICOLON, PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(SEMICOLON, PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_BEFORE_SEMICOLON);

        // Import
        if (custom.IMPORT_TRIM_TAIL) {
            builder.after(IMPORT_COMMAND).spaces(0);
        }

        // Context
        if (custom.CONTEXT_TRIM_TAIL) {
            builder.afterInside(VAR_DECLARATION, CONTEXT).spaces(0);
        }

        // Operators
        builder.before(VAR_ACCUMULATOR_TYPE).spaces(1);
        builder.after(VAR_ACCUMULATOR_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        builder.around(VAR_ASSIGNMENT_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        builder.around(COLUMN_ASSIGNMENT_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);

        // Operations
        // Special case - separate apply always
        builder.before(OPERATOR_CUT).spaces(1);
        builder.after(OPERATOR_CUT).spaceIf(custom.SPACE_AFTER_OPERATOR_CUT);
        builder.around(OPERATOR_APPLY).spaces(1);

        builder.aroundInside(OPERATOR_ARITHMETIC, PREFIX_INVOKE_EXPR).spaces(0);
        builder.around(OPERATOR_ARITHMETIC).spaceIf(custom.SPACE_AROUND_OPERATOR_ARITHMETIC);
        builder.aroundInside(OPERATOR_ORDER, PREFIX_INVOKE_EXPR).spaces(0);
        builder.around(OPERATOR_ORDER).spaceIf(custom.SPACE_AROUND_OPERATOR_ORDER);
        builder.aroundInside(OPERATOR_EQUALITY, PREFIX_INVOKE_EXPR).spaces(0);
        builder.around(OPERATOR_EQUALITY).spaceIf(custom.SPACE_AROUND_OPERATOR_EQUALITY);
        builder.aroundInside(OPERATOR_WEIGHT, PREFIX_INVOKE_EXPR).spaces(0);
        builder.around(OPERATOR_WEIGHT).spaceIf(custom.SPACE_AROUND_OPERATOR_WEIGHT);
        builder.aroundInside(OPERATOR_OTHERS, PREFIX_INVOKE_EXPR).spaces(0);
        builder.around(OPERATOR_OTHERS).spaceIf(custom.SPACE_AROUND_OPERATOR_OTHERS);
        builder.aroundInside(OPERATOR_COMMA, PREFIX_INVOKE_EXPR).spaces(0);
        builder.after(OPERATOR_COMMA).spaceIf(custom.SPACE_AFTER_OPERATOR_COMMA);
        builder.before(OPERATOR_COMMA).spaces(0);

        // Mode
        builder.after(MODE_PATTERN).spaceIf(custom.MODE_SPACE_AFTER);

        // Query
        builder.after(QUERY_TYPE).spaces(1);
        builder.before(QUERY_BY).spaces(1);
        builder.after(QUERY_BY).spaces(1);
        builder.before(QUERY_FROM).spaces(1);
        builder.after(QUERY_FROM).spaces(1);
        builder.before(QUERY_WHERE).spaces(1);
        builder.after(QUERY_WHERE).spaces(1);
        builder.before(QUERY_SPLITTER).spaces(0);
        builder.after(QUERY_SPLITTER).spaceIf(custom.QUERY_SPACE_AFTER_COMMA);

        // Predefined
        builder.after(COMMAND_SYSTEM).spaces(1);
        builder.after(COMMAND_IMPORT).spaces(1);
        builder.after(FUNCTION_IMPORT).spaces(1);

        // InvokeFunction expanded
        builder.after(INTERNAL_FUNCTION).spaces(0); // Special case for internal functions, like -11!x
        for (IElementType type : List.of(UNARY_FUNCTION, BINARY_FUNCTION, COMPLEX_FUNCTION, INTERNAL_FUNCTION)) {
            builder.between(type, ARGUMENTS).spaces(0);
            builder.between(type, ITERATOR_TYPE).spaces(0);
            builder.between(type, SYMBOL).spaces(1);
            builder.between(type, SYMBOLS).spaces(1);
            builder.after(type).spaces(1);
        }

        builder.between(CUSTOM_FUNCTION, ARGUMENTS).spaces(0);
        builder.between(CUSTOM_FUNCTION, ITERATOR_TYPE).spaces(0);
        builder.between(CUSTOM_FUNCTION, SYMBOL).spaceIf(custom.FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL);
        builder.between(CUSTOM_FUNCTION, SYMBOLS).spaceIf(custom.FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL);
        builder.after(CUSTOM_FUNCTION).spaces(1);

        // Others
        builder.after(ITERATOR_TYPE).spaceIf(custom.ITERATOR_SPACE_AFTER);
        builder.beforeInside(VAR_DECLARATION, CONTEXT).spaces(1);
        builder.afterInside(COLON, RETURN_EXPR).spaceIf(custom.RETURN_SPACE_AFTER_COLON);
        builder.afterInside(ITERATOR, SIGNAL_EXPR).spaceIf(custom.SIGNAL_SPACE_AFTER_SIGNAL);
        builder.between(SEMICOLON, LINE_COMMENT).spacing(0, Integer.MAX_VALUE, 0, true, common.KEEP_BLANK_LINES_IN_CODE);
        builder.after(SEMICOLON).spaceIf(custom.SEMICOLON_SPACE_AFTER);
        builder.before(SEMICOLON).spacing(0, custom.EXPRESSION_SEMICOLON_TRIM_SPACES ? 0 : Integer.MAX_VALUE, 0, !custom.EXPRESSION_SEMICOLON_REMOVE_LINES, custom.EXPRESSION_SEMICOLON_REMOVE_LINES ? 0 : common.KEEP_BLANK_LINES_IN_CODE);
    }

    public Spacing getSpacing(Block parent, Block child1, Block child2) {
        if (!(parent instanceof ASTBlock) || !(child1 instanceof ASTBlock) || !(child2 instanceof ASTBlock)) {
            return null;
        }
        return getASTSpacing((ASTBlock) parent, (ASTBlock) child1, (ASTBlock) child2);
    }

    private Spacing getASTSpacing(ASTBlock parent, ASTBlock child1, ASTBlock child2) {
        return builder.getSpacing(parent, child1, child2);
    }
}
