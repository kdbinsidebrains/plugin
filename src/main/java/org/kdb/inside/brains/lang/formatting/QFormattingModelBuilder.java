package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.formatting.blocks.CodeBlock;

public class QFormattingModelBuilder implements FormattingModelBuilder {
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final PsiElement element = formattingContext.getPsiElement();

        final QFormatter formatter = new QFormatter(formattingContext);
        final CodeStyleSettings settings = formattingContext.getCodeStyleSettings();

        final Block rootBlock = new CodeBlock(element.getNode(), formatter);

        // TODO: REMOVE THIS LINE
        System.out.println(FormattingModelDumper.dumpFormattingModelToString(rootBlock));

        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), rootBlock, settings);
    }

    @Nullable
    @Override
    public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
        return null;
    }
}
