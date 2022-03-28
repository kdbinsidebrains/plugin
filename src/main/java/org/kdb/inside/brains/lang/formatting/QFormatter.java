package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.ASTBlock;
import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.Spacing;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.kdb.inside.brains.QLanguage;

public class QFormatter {
    public final QSpacingStrategy spacing;
    public final QCodeStyleSettings custom;
    public final CommonCodeStyleSettings common;

    public QFormatter(FormattingContext formattingContext) {
        this(formattingContext.getCodeStyleSettings());
    }

    private QFormatter(CodeStyleSettings settings) {
        this(new QSpacingStrategy(settings), settings.getCustomSettings(QCodeStyleSettings.class), settings.getCommonSettings(QLanguage.INSTANCE));
    }

    private QFormatter(QSpacingStrategy spacing, QCodeStyleSettings custom, CommonCodeStyleSettings common) {
        this.spacing = spacing;
        this.custom = custom;
        this.common = common;
    }

    public Spacing getSpacing(ASTBlock parent, Block child1, Block child2) {
        return spacing.getSpacing(parent, child1, child2);
    }
}