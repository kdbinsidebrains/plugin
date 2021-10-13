package org.kdb.inside.brains.lang.codestyle;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QTypes;

public class QSpacingStrategy {
    private SpacingBuilder builder;

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        final CommonCodeStyleSettings settings = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);
        final QCodeStyleSettings qSettings = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Control
        builder.after(QTypes.CONTROL_KEYWORD).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(qSettings.CONTROL_OBRACKET_ON_NEXT_LINE, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(qSettings.CONTROL_CBRACKET_ON_NEXT_LINE, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONTROL_EXPR).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_BEFORE_SEMICOLON);

        // Condition
        builder.after(QTypes.CONDITION_KEYWORD).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(qSettings.CONDITION_OBRACKET_ON_NEXT_LINE, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(qSettings.CONDITION_CBRACKET_ON_NEXT_LINE, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONDITION_EXPR).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_BEFORE_SEMICOLON);

        // Import
        if (qSettings.IMPORT_TRIM_TAIL) {
            builder.after(QTypes.IMPORT_COMMAND).spaces(0);
        }
        // Context
        if (qSettings.CONTEXT_TRIM_TAIL) {
            builder.afterInside(QTypes.VAR_DECLARATION, QTypes.CONTEXT).spaces(0);
        }

        // Operators
        builder.around(QTypes.ASSIGNMENT_EXPR).spaceIf(qSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS);
        builder.aroundInside(QTypes.OPERATOR_ARITHMETIC, QTypes.EXPRESSION).spaceIf(qSettings.SPACE_AROUND_ARITHMETIC_OPERATORS);

        // Parameters
        builder.afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(qSettings.LAMBDA_SPACE_AFTER_PARAMETERS);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE);

        // Remove spaces before last semicolon in all other cases
        builder.before(QTypes.SEMICOLON).spaceIf(false);
    }

    public Spacing getSpacing(Block parent, Block child1, Block child2) {
        if (!(parent instanceof AbstractBlock) || !(child1 instanceof AbstractBlock) || !(child2 instanceof AbstractBlock)) {
            return null;
        }
        return getASTSpacing((AbstractBlock) parent, (AbstractBlock) child1, (AbstractBlock) child2);
    }

    private Spacing getASTSpacing(AbstractBlock parent, AbstractBlock child1, AbstractBlock child2) {
        return builder.getSpacing(parent, child1, child2);
    }
}
