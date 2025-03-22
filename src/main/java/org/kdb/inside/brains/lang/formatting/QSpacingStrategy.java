package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.lang.QNodeFactory;

import static org.kdb.inside.brains.psi.QTypes.*;

public class QSpacingStrategy {
    private final SpacingBuilder builder;
    private final QCodeStyleSettings custom;
    private final CommonCodeStyleSettings common;

    private static final TokenSet SYMBOLS_SET = TokenSet.create(SYMBOL, SYMBOLS);
    private static final TokenSet FUNCTIONS_SET = TokenSet.create(UNARY_FUNCTION, BINARY_FUNCTION, COMPLEX_FUNCTION, INTERNAL_FUNCTION);
    private static final TokenSet OPERATORS_SET = TokenSet.create(OPERATOR_CUT, OPERATOR_ARITHMETIC, OPERATOR_ORDER, OPERATOR_EQUALITY, OPERATOR_WEIGHT, OPERATOR_OTHERS, OPERATOR_COMMA);

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        custom = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);
        common = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Lambda
        builder.afterInside(COLON, TYPED_PARAMETER).spaceIf(custom.LAMBDA_SPACE_AROUND_TYPED_PARAMS_COLON);
        builder.beforeInside(COLON, TYPED_PARAMETER).spaceIf(custom.LAMBDA_SPACE_AROUND_TYPED_PARAMS_COLON);
        builder.afterInside(SEMICOLON, PATTERN_PARAMETER).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMS_SEMICOLON);
        builder.beforeInside(SEMICOLON, PATTERN_PARAMETER).spaceIf(custom.LAMBDA_SPACE_BEFORE_PARAMS_SEMICOLON);
        builder.afterInside(PAREN_OPEN, PATTERN_PARAMETER).spaceIf(custom.LAMBDA_SPACE_PATTERN_PARAMS_WITHIN_PARENS);
        builder.beforeInside(PAREN_CLOSE, PATTERN_PARAMETER).spaceIf(custom.LAMBDA_SPACE_PATTERN_PARAMS_WITHIN_PARENS);
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
        builder.between(ARGUMENTS, ARGUMENTS).none();
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
        builder.around(VAR_ASSIGNMENT_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        builder.around(VAR_ACCUMULATOR_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        builder.around(COLUMN_ASSIGNMENT_TYPE).spaceIf(custom.SPACE_AROUND_ASSIGNMENT_OPERATORS);

        // Special case - cut can't go between a var
        builder.between(VAR_REFERENCE, OPERATOR_CUT).spaces(1);

        // Operations
        builder.around(OPERATOR_APPLY).spaces(1);
        builder.around(OPERATOR_CUT).spaceIf(custom.SPACE_AROUND_OPERATOR_CUT);
        builder.around(OPERATOR_ARITHMETIC).spaceIf(custom.SPACE_AROUND_OPERATOR_ARITHMETIC);
        builder.around(OPERATOR_ORDER).spaceIf(custom.SPACE_AROUND_OPERATOR_ORDER);
        builder.around(OPERATOR_EQUALITY).spaceIf(custom.SPACE_AROUND_OPERATOR_EQUALITY);
        builder.around(OPERATOR_WEIGHT).spaceIf(custom.SPACE_AROUND_OPERATOR_WEIGHT);
        builder.around(OPERATOR_OTHERS).spaceIf(custom.SPACE_AROUND_OPERATOR_OTHERS);

        builder.before(OPERATOR_COMMA).spaces(0);
        builder.after(OPERATOR_COMMA).spaceIf(custom.SPACE_AFTER_OPERATOR_COMMA);
        for (IElementType operator : OPERATORS_SET.getTypes()) {
            builder.aroundInside(operator, INVOKE_PREFIX).spaces(0);
        }

        // Iterators
        // @see #iteratorSpacing

        // Mode
        builder.after(MODE_PATTERN).spaceIf(custom.MODE_SPACE_AFTER);

        // Query
        builder.after(QUERY_TYPE).spaces(1);
        builder.around(QUERY_BY).spaces(1);
        builder.around(QUERY_FROM).spaces(1);
        builder.around(QUERY_WHERE).spaces(1);
        builder.before(QUERY_SPLITTER).spaces(0);
        builder.after(QUERY_SPLITTER).spaceIf(custom.QUERY_SPACE_AFTER_COMMA);

        // Predefined
        builder.after(COMMAND_SYSTEM).spaces(1);
        builder.after(COMMAND_IMPORT).spaces(1);
        builder.after(FUNCTION_IMPORT).spaces(1);

        // InvokeFunction expanded
        builder.after(INTERNAL_FUNCTION).spaceIf(custom.EXECUTION_SPACE_AFTER_INTERNAL); // Special case for internal functions, like -11!x
        // Operator types
        for (IElementType type : FUNCTIONS_SET.getTypes()) {
            builder.between(type, ARGUMENTS).spaceIf(custom.EXECUTION_SPACE_BEFORE_ARGUMENTS);
            builder.between(type, SYMBOL).spaceIf(custom.EXECUTION_SPACE_BEFORE_SYMBOLS);
            builder.between(type, SYMBOLS).spaceIf(custom.EXECUTION_SPACE_BEFORE_SYMBOLS);
            builder.after(type).spaceIf(custom.EXECUTION_SPACE_BEFORE_PARAMETER);
        }

        builder.between(CUSTOM_FUNCTION, ARGUMENTS).spaces(0);
        builder.between(CUSTOM_FUNCTION, ITERATOR_TYPE).spaces(0);
        builder.between(CUSTOM_FUNCTION, SYMBOL).spaceIf(custom.FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL);
        builder.between(CUSTOM_FUNCTION, SYMBOLS).spaceIf(custom.FUNCTION_INVOKE_SPACE_BEFORE_SYMBOL);
        builder.after(CUSTOM_FUNCTION).spaces(1);

        // Others
        builder.around(ITERATOR_TYPE).spaces(0);
        builder.afterInside(ARGUMENTS, INVOKE_PREFIX).spaces(1);
        builder.afterInside(ARGUMENTS, INVOKE_FUNCTION).spaces(1);
        builder.afterInside(ARGUMENTS, INVOKE_PARENTHESES).spaces(1);
        builder.beforeInside(VAR_DECLARATION, CONTEXT).spaces(1);
        builder.afterInside(COLON, RETURN_EXPR).spaceIf(custom.RETURN_SPACE_AFTER_COLON);
        builder.afterInside(ITERATOR, SIGNAL_EXPR).spaceIf(custom.SIGNAL_SPACE_AFTER_SIGNAL);
        builder.before(LINE_COMMENT).spacing(1, custom.LINE_COMMENT_TRIM_SPACES ? 1 : Integer.MAX_VALUE, 0, common.KEEP_LINE_BREAKS, common.KEEP_BLANK_LINES_IN_CODE);
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
        final Spacing spacing = customSpacing(child1, child2);
        if (spacing != null) {
            return spacing;
        }
        return builder.getSpacing(parent, child1, child2);
    }

    public Spacing spacing(int count) {
        return spacing(count, (ASTNode) null);
    }

    public Spacing spacingIf(boolean condition) {
        return spacing(condition ? 1 : 0);
    }

    public Spacing spacing(int count, Block parent) {
        return spacing(count, parent == null ? null : ASTBlock.getNode(parent));
    }

    public Spacing spacing(int count, ASTNode parent) {
        if (parent != null) {
            return Spacing.createDependentLFSpacing(count, count, parent.getTextRange(), common.KEEP_LINE_BREAKS, common.KEEP_BLANK_LINES_IN_CODE);
        } else {
            return Spacing.createSpacing(count, count, 0, common.KEEP_LINE_BREAKS, common.KEEP_BLANK_LINES_IN_CODE);
        }
    }

    private Spacing customSpacing(ASTBlock child1, ASTBlock child2) {
        final IElementType child1Type = ASTBlock.getElementType(child1);
        final IElementType child2Type = ASTBlock.getElementType(child2);

        final Spacing spacing = operatorCutFix(child1, child1Type, child2, child2Type);
        if (spacing != null) {
            return spacing;
        }
        return iteratorSpacing(child2, child1Type, child2Type);
    }

    @Nullable
    private Spacing iteratorSpacing(ASTBlock child2, IElementType child1Type, IElementType child2Type) {
        if (child1Type == ITERATOR_TYPE && child2Type != ITERATOR_TYPE) {
            return spacingIf(custom.ITERATOR_SPACE_AROUND);
        }

        if (child2Type == ITERATOR_TYPE) {
            final ASTNode iter = QNodeFactory.getFirstNotEmptyChild(child2.getNode());
            if (iter != null && iter.getText().charAt(0) == '/') {
                return spacing(0);
            }

            if (child1Type == ITERATOR_TYPE) {
                return spacingIf(custom.ITERATOR_SPACE_BETWEEN);
            }

            if (OPERATORS_SET.contains(child1Type)) {
                return spacingIf(custom.ITERATOR_SPACE_AFTER_OPERATOR);
            }
            return spacingIf(custom.ITERATOR_SPACE_AROUND);
        }
        return null;
    }

    @Nullable
    private Spacing operatorCutFix(ASTBlock child1, IElementType child1Type, ASTBlock child2, IElementType child2Type) {
        if (child2Type == VAR_ACCUMULATOR_TYPE) {
            final ASTNode node = child2.getNode();
            if (node != null && node.getTextLength() != 0 && node.getChars().charAt(0) == '_') {
                return spacing(1);
            }
            return null;
        }

        if (child2Type != OPERATOR_CUT) {
            return null;
        }

        if (child1Type == CUSTOM_FUNCTION) {
            final ASTNode funcChild = QNodeFactory.getFirstNotEmptyChild(child1.getNode());
            if (funcChild == null) {
                return null;
            }

            if (funcChild.getElementType() == VAR_REFERENCE) {
                return spacing(1);
            }
            if (funcChild.getElementType() == LITERAL_EXPR && funcChild.findChildByType(SYMBOLS_SET) != null) {
                return spacing(1);
            }
        }
        return null;
    }
}
