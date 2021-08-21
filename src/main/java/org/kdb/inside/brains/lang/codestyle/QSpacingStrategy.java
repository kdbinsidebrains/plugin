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
    private final SpacingBuilder builder;

    public QSpacingStrategy(@NotNull CodeStyleSettings codeStyleSettings) {
        final CommonCodeStyleSettings settings = codeStyleSettings.getCommonSettings(QLanguage.INSTANCE);
        final QCodeStyleSettings qSettings = codeStyleSettings.getCustomSettings(QCodeStyleSettings.class);

        builder = new SpacingBuilder(codeStyleSettings, QLanguage.INSTANCE)
/*
                .after(QTypes.MODE_PATTERN).spaceIf(true)
                .between(QTypes.SEMICOLON, QTypes.EXPRESSION).spaceIf(true)
                .between(QTypes.EXPRESSION, QTypes.SEMICOLON).spaceIf(false)
                .between(QTypes.SEMICOLON, QTypes.VARIABLE_PATTERN).spaceIf(true)
                .betweenInside(QTypes.BRACE_OPEN, QTypes.PARAMETERS, QTypes.LAMBDA).spacing(0, 0, 0, settings.KEEP_LINE_BREAKS, 10);
*/
/*
        myResult = Spacing.createDependentLFSpacing(spaces, spaces, dependence, mySettings.KEEP_LINE_BREAKS,
                mySettings.KEEP_BLANK_LINES_IN_CODE);
*/

//                .afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf()
                // Around operators
                .after(QTypes.ASSIGNMENT_OPERATOR).spaceIf(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
                .before(QTypes.ASSIGNMENT_OPERATOR).spaceIf(settings.SPACE_AROUND_ASSIGNMENT_OPERATORS)

                // Parameters
                .afterInside(QTypes.SEMICOLON, QTypes.PARAMETERS).spaceIf(qSettings.SPACE_AFTER_LAMBDA_PARAMETERS)
                .afterInside(QTypes.BRACKET_OPEN, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE)
                .beforeInside(QTypes.BRACKET_CLOSE, QTypes.PARAMETERS).lineBreakInCodeIf(settings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE)
        ;
    }

    public Spacing getSpacing(Block parent, Block child1, Block child2) {
        if (!(parent instanceof AbstractBlock) || !(child1 instanceof AbstractBlock) || !(child2 instanceof AbstractBlock)) {
            return null;
        }
        return getASTSpacing((AbstractBlock) parent, (AbstractBlock) child1, (AbstractBlock) child2);
    }

    public Spacing getASTSpacing(AbstractBlock parent, AbstractBlock child1, AbstractBlock child2) {
        return builder.getSpacing(parent, child1, child2);
    }
}
