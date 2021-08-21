package org.kdb.inside.brains.lang.codestyle;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.lang.codestyle.blocks.SimpleQBlock;

public class QFormattingModelBuilder implements FormattingModelBuilder {
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final PsiElement element = formattingContext.getPsiElement();

        final CodeStyleSettings settings = formattingContext.getCodeStyleSettings();

        final QSpacingStrategy spacingStrategy = new QSpacingStrategy(settings);
        final QCodeStyleSettings qSettings = settings.getCustomSettings(QCodeStyleSettings.class);
        final CommonCodeStyleSettings commonSettings = settings.getCommonSettings(QLanguage.INSTANCE);

        final Block rootBlock = new SimpleQBlock(element.getNode(), spacingStrategy, qSettings, commonSettings, null, null, null);

        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), rootBlock, settings);
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}
