package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.TokenSet;
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
        builder.afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(custom.PARAMS_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(custom.PARAMS_SPACE_BEFORE_SEMICOLON);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).spaceIf(custom.PARAMS_SPACE_WITHIN_BRACKET, custom.PARAMS_LBRACKET_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).spaceIf(custom.PARAMS_SPACE_WITHIN_BRACKET, custom.PARAMS_RBRACKET_ON_NEXT_LINE);

        // Lambda definition
        builder.afterInside(QTypes.PARAMETERS, QTypes.LAMBDA_EXPR).spaceIf(custom.LAMBDA_SPACE_AFTER_PARAMETERS);
//        builder.betweenInside(QTypes.BRACE_OPEN, QTypes.PARAMETERS, QTypes.LAMBDA_EXPR).spaces(0);

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
        builder.around(QTypes.OPERATOR_ARITHMETIC).spaceIf(custom.SPACE_AROUND_OPERATOR_ARITHMETIC);
        builder.around(QTypes.OPERATOR_ORDER).spaceIf(custom.SPACE_AROUND_OPERATOR_ORDER);
        builder.around(QTypes.OPERATOR_EQUALITY).spaceIf(custom.SPACE_AROUND_OPERATOR_EQUALITY);
        builder.around(QTypes.OPERATOR_WEIGHT).spaceIf(custom.SPACE_AROUND_OPERATOR_WEIGHT);
        builder.around(QTypes.OPERATOR_COMMA).spaceIf(custom.SPACE_AROUND_OPERATOR_OTHERS);
        builder.around(QTypes.OPERATOR_OTHERS).spaceIf(custom.SPACE_AROUND_OPERATOR_OTHERS);

        // Semicolon
        builder.before(QTypes.SEMICOLON).spacing(0, custom.EXPRESSION_SEMICOLON_TRIM_SPACES ? 0 : Integer.MAX_VALUE, 0, !custom.EXPRESSION_SEMICOLON_REMOVE_LINES, custom.EXPRESSION_SEMICOLON_REMOVE_LINES ? 0 : common.KEEP_BLANK_LINES_IN_CODE);

        // Tables
        builder.betweenInside(QTypes.PAREN_OPEN, QTypes.BRACKET_OPEN, QTypes.TABLE_EXPR).spaces(0);
        builder.betweenInside(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, QTypes.TABLE_EXPR).spaces(0);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.TABLE_EXPR).spaces(0);
        builder.betweenInside(QTypes.BRACKET_CLOSE, QTypes.SEMICOLON, QTypes.TABLE_EXPR).spaces(0); // no spaces before semicolon
        builder.afterInside(QTypes.BRACKET_CLOSE, QTypes.TABLE_EXPR).spaceIf(custom.TABLE_SPACE_AFTER_KEY_COLUMNS);
        builder.around(TokenSet.create(QTypes.TABLE_KEYS, QTypes.TABLE_VALUES, QTypes.TABLE_COLUMN)).spacing(0, 1, 0, true, 0);

        // Execution
        builder.after(QTypes.OPERATOR_EXECUTE).spaces(1);
        builder.before(QTypes.OPERATOR_EXECUTE).spaceIf(custom.CONTROL_SPACE_BEFORE_EXECUTION);
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
