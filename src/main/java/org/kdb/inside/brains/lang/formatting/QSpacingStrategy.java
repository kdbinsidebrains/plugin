package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QTypes;

public class QSpacingStrategy {
    private final SpacingBuilder builder;

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        final QCodeStyleSettings custom = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);
        final CommonCodeStyleSettings common = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Lambda parameters
        builder.afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(custom.LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).spaceIf(custom.LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS, custom.LAMBDA_PARAMS_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).spaceIf(custom.LAMBDA_SPACE_WITHIN_PARAMS_BRACKETS, custom.LAMBDA_PARAMS_RBRACKET_ON_NEXT_LINE);

        // Lambda definition
        builder.afterInside(QTypes.PARAMETERS, QTypes.LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMETERS);
        builder.betweenInside(QTypes.BRACE_OPEN, QTypes.EXPRESSIONS, QTypes.LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_WITHIN_BRACES);
        builder.betweenInside(QTypes.EXPRESSIONS, QTypes.BRACE_CLOSE, QTypes.LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_WITHIN_BRACES);

        // Control
        builder.after(QTypes.CONTROL_KEYWORD).spaceIf(custom.CONTROL_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_WITHIN_BRACES, custom.CONTROL_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_WITHIN_BRACES, custom.CONTROL_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).spaceIf(custom.CONTROL_SPACE_BEFORE_SEMICOLON);

        // Condition
        builder.after(QTypes.CONDITION_KEYWORD).spaceIf(custom.CONDITION_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_WITHIN_BRACES, custom.CONDITION_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_WITHIN_BRACES, custom.CONDITION_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).spaceIf(custom.CONDITION_SPACE_BEFORE_SEMICOLON);

        // Arguments
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_WITHIN_BRACES, custom.ARGUMENTS_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_WITHIN_BRACES, custom.ARGUMENTS_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(QTypes.SEMICOLON, QTypes.ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.ARGUMENTS).spaceIf(custom.ARGUMENTS_SPACE_BEFORE_SEMICOLON);

        // Grouping
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_WITHIN_BRACES, custom.GROUPING_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_WITHIN_BRACES, custom.GROUPING_RBRACKET_ON_NEXT_LINE);
        builder.afterInside(QTypes.SEMICOLON, QTypes.GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.GROUPING_EXPR).spaceIf(custom.GROUPING_SPACE_BEFORE_SEMICOLON);

        // Parentheses
        builder.afterInside(QTypes.PAREN_OPEN, QTypes.PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_WITHIN_PARENS, custom.PARENTHESES_LPAREN_ON_NEXT_LINE);
        builder.beforeInside(QTypes.PAREN_CLOSE, QTypes.PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_WITHIN_PARENS, custom.PARENTHESES_RPAREN_ON_NEXT_LINE);
        builder.afterInside(QTypes.SEMICOLON, QTypes.PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.PARENTHESES_EXPR).spaceIf(custom.PARENTHESES_SPACE_BEFORE_SEMICOLON);

        // Import
        if (custom.IMPORT_TRIM_TAIL) {
            builder.after(QTypes.IMPORT_COMMAND).spaces(0);
        }

        // Context
        if (custom.CONTEXT_TRIM_TAIL) {
            builder.afterInside(QTypes.VAR_DECLARATION, QTypes.CONTEXT).spaces(0);
        }

        // Operators
        builder.around(QTypes.ASSIGNMENT_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);

        // Operations
        builder.aroundInside(QTypes.OPERATOR_ARITHMETIC, QTypes.PREFIX_INVOKE_EXPR).spaces(0);
        builder.aroundInside(QTypes.OPERATOR_ORDER, QTypes.PREFIX_INVOKE_EXPR).spaces(0);
        builder.aroundInside(QTypes.OPERATOR_EQUALITY, QTypes.PREFIX_INVOKE_EXPR).spaces(0);
        builder.aroundInside(QTypes.OPERATOR_WEIGHT, QTypes.PREFIX_INVOKE_EXPR).spaces(0);
        builder.aroundInside(QTypes.OPERATOR_COMMA, QTypes.PREFIX_INVOKE_EXPR).spaces(0);
        builder.aroundInside(QTypes.OPERATOR_OTHERS, QTypes.PREFIX_INVOKE_EXPR).spaces(0);

        builder.around(QTypes.OPERATOR_ARITHMETIC).spaceIf(custom.SPACE_AROUND_OPERATOR_ARITHMETIC);
        builder.around(QTypes.OPERATOR_ORDER).spaceIf(custom.SPACE_AROUND_OPERATOR_ORDER);
        builder.around(QTypes.OPERATOR_EQUALITY).spaceIf(custom.SPACE_AROUND_OPERATOR_EQUALITY);
        builder.around(QTypes.OPERATOR_WEIGHT).spaceIf(custom.SPACE_AROUND_OPERATOR_WEIGHT);
        builder.around(QTypes.OPERATOR_OTHERS).spaceIf(custom.SPACE_AROUND_OPERATOR_OTHERS);
        builder.after(QTypes.OPERATOR_COMMA).spaceIf(custom.SPACE_AFTER_OPERATOR_COMMA);
        builder.before(QTypes.OPERATOR_COMMA).spaces(0);

        // Others
        builder.after(QTypes.OPERATOR_EXECUTE).spaces(1);
        builder.beforeInside(QTypes.VAR_DECLARATION, QTypes.CONTEXT).spaces(1);
        builder.before(QTypes.OPERATOR_EXECUTE).spaceIf(custom.CONTROL_SPACE_BEFORE_EXECUTION);
        builder.afterInside(QTypes.COLON, QTypes.RETURN_EXPR).spaceIf(custom.RETURN_SPACE_AFTER_COLON);
        builder.afterInside(QTypes.ITERATOR, QTypes.SIGNAL_EXPR).spaceIf(custom.SIGNAL_SPACE_AFTER_SIGNAL);
        builder.before(QTypes.SEMICOLON).spacing(0, custom.EXPRESSION_SEMICOLON_TRIM_SPACES ? 0 : Integer.MAX_VALUE, 0, !custom.EXPRESSION_SEMICOLON_REMOVE_LINES, custom.EXPRESSION_SEMICOLON_REMOVE_LINES ? 0 : common.KEEP_BLANK_LINES_IN_CODE);

        // Mode
        builder.after(QTypes.MODE_PATTERN).spaceIf(custom.MODE_SPACE_AFTER);

        // Query
        builder.after(QTypes.QUERY_TYPE).spaces(1);
        builder.before(QTypes.QUERY_BY).spaces(1);
        builder.after(QTypes.QUERY_BY).spaces(1);
        builder.before(QTypes.QUERY_FROM).spaces(1);
        builder.after(QTypes.QUERY_FROM).spaces(1);
        builder.before(QTypes.QUERY_WHERE).spaces(1);
        builder.after(QTypes.QUERY_WHERE).spaces(1);
        builder.before(QTypes.QUERY_SPLITTER).spaces(0);
        builder.after(QTypes.QUERY_SPLITTER).spaceIf(custom.QUERY_SPACE_AFTER_COMMA);

        // Predefined
        builder.after(QTypes.COMMAND_SYSTEM).spaces(1);
        builder.after(QTypes.COMMAND_IMPORT).spaces(1);
        builder.after(QTypes.FUNCTION_IMPORT).spaces(1);

        builder.between(QTypes.INVOKE_FUNCTION, QTypes.ARGUMENTS).spaces(0);
        builder.between(QTypes.INVOKE_FUNCTION, QTypes.ITERATOR_TYPE).spaces(0);
        builder.after(QTypes.INVOKE_FUNCTION).spaces(1);
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
