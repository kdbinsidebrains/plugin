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
    private final QCodeStyleSettings qSettings;
    private final CommonCodeStyleSettings settings;

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        settings = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);
        qSettings = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE);

        // Control
        builder.after(QTypes.CONTROL_PATTERN).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONTROL).lineBreakOrForceSpace(qSettings.CONTROL_OBRACKET_ON_NEXT_LINE, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONTROL).lineBreakOrForceSpace(qSettings.CONTROL_CBRACKET_ON_NEXT_LINE, qSettings.CONTROL_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONTROL).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONTROL).lineBreakOrForceSpace(false, qSettings.CONTROL_SPACE_BEFORE_SEMICOLON);

        // Condition
        builder.after(QTypes.CONDITION_PATTERN).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_OPERATOR);
        builder.afterInside(QTypes.BRACKET_OPEN, QTypes.CONDITION).lineBreakOrForceSpace(qSettings.CONDITION_OBRACKET_ON_NEXT_LINE, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.beforeInside(QTypes.BRACKET_CLOSE, QTypes.CONDITION).lineBreakOrForceSpace(qSettings.CONDITION_CBRACKET_ON_NEXT_LINE, qSettings.CONDITION_SPACE_WITHIN_BRACES);
        builder.afterInside(QTypes.SEMICOLON, QTypes.CONDITION).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_AFTER_SEMICOLON);
        builder.beforeInside(QTypes.SEMICOLON, QTypes.CONDITION).lineBreakOrForceSpace(false, qSettings.CONDITION_SPACE_BEFORE_SEMICOLON);


        // Around operators
        builder = builder
                .around(QTypes.ASSIGNMENT_OPERATOR).spaceIf(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
        ;

        // Parameters
        builder = builder
                .afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(qSettings.LAMBDA_SPACE_AFTER_PARAMETERS)
                .afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE)
                .beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE)
        ;

        // Remove spaces before last semicolon in all other cases
        builder = builder
                .before(QTypes.SEMICOLON).spaceIf(false)
        ;
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
