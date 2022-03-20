package org.kdb.inside.brains.lang.codestyle;

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
        final CommonCodeStyleSettings settings = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);
        final QCodeStyleSettings qSettings = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Control
        builder.after(QTypes.CONTROL_KEYWORD).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(qSettings.CONTROL_OBRACKET_ON_NEXT_LINE && qSettings.CONTROL_WRAP_TYPE != CommonCodeStyleSettings.DO_NOT_WRAP, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(qSettings.CONTROL_CBRACKET_ON_NEXT_LINE && qSettings.CONTROL_WRAP_TYPE != CommonCodeStyleSettings.DO_NOT_WRAP, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_BEFORE_SEMICOLON);

        // Condition
        builder.after(QTypes.CONDITION_KEYWORD).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(qSettings.CONDITION_OBRACKET_ON_NEXT_LINE && qSettings.CONDITION_WRAP_TYPE != CommonCodeStyleSettings.DO_NOT_WRAP, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(qSettings.CONDITION_CBRACKET_ON_NEXT_LINE && qSettings.CONDITION_WRAP_TYPE != CommonCodeStyleSettings.DO_NOT_WRAP, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_BEFORE_SEMICOLON);

        // Lambdas
        builder.afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(qSettings.LAMBDA_SPACE_AFTER_PARAMETERS);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE && settings.METHOD_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE && settings.METHOD_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP);

        // Import
        if (qSettings.IMPORT_TRIM_TAIL) {
            builder.after(QTypes.IMPORT_COMMAND).spaces(0);
        }

        // Context
        if (qSettings.CONTEXT_TRIM_TAIL) {
            builder.afterInside(QTypes.VAR_DECLARATION, QTypes.CONTEXT).spaces(0);
        }

        // Operators
        builder.around(QTypes.ASSIGNMENT_TYPE).spaceIf(qSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);

        // Tables
        builder.betweenInside(QTypes.PAREN_OPEN, QTypes.BRACKET_OPEN, QTypes.TABLE_EXPR).spaces(0);
        builder.betweenInside(QTypes.BRACKET_OPEN, QTypes.BRACKET_CLOSE, QTypes.TABLE_EXPR).spaces(0);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.TABLE_EXPR).spaces(0);
        builder.betweenInside(QTypes.BRACKET_CLOSE, QTypes.SEMICOLON, QTypes.TABLE_EXPR).spaces(0); // no spaces before semicolon
        builder.afterInside(QTypes.BRACKET_CLOSE, QTypes.TABLE_EXPR).spaceIf(qSettings.TABLE_SPACE_AFTER_KEY_COLUMNS);
        builder.around(TokenSet.create(QTypes.TABLE_KEYS, QTypes.TABLE_VALUES, QTypes.TABLE_COLUMN)).spacing(0, 1, 0, true, 0);

        // Execution
        builder.after(QTypes.OPERATOR_EXECUTE).spaces(1);
        builder.before(QTypes.OPERATOR_EXECUTE).spaceIf(qSettings.CONTROL_SPACE_BEFORE_EXECUTION);

        // Operations
        builder.around(QTypes.OPERATOR_ARITHMETIC).spaceIf(qSettings.SPACE_AROUND_OPERATOR_ARITHMETIC);
        builder.around(QTypes.OPERATOR_ORDER).spaceIf(qSettings.SPACE_AROUND_OPERATOR_ORDER);
        builder.around(QTypes.OPERATOR_EQUALITY).spaceIf(qSettings.SPACE_AROUND_OPERATOR_EQUALITY);
        builder.around(QTypes.OPERATOR_WEIGHT).spaceIf(qSettings.SPACE_AROUND_OPERATOR_WEIGHT);
        builder.around(QTypes.OPERATOR_COMMA).spaceIf(qSettings.SPACE_AROUND_OPERATOR_OTHERS);
        builder.around(QTypes.OPERATOR_OTHERS).spaceIf(qSettings.SPACE_AROUND_OPERATOR_OTHERS);

        builder.before(QTypes.SEMICOLON).spacing(0, 0, 0, qSettings.EXPRESSION_SEMICOLON_ON_NEW_LINE, 0);
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
