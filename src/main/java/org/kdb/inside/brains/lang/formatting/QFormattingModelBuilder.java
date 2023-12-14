package org.kdb.inside.brains.lang.formatting;

import com.intellij.formatting.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.formatting.blocks.CodeBlock;

public class QFormattingModelBuilder implements FormattingModelBuilder {
    private static final Logger log = Logger.getInstance(QFormattingModelBuilder.class);

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final PsiElement element = formattingContext.getPsiElement();

        final QFormatter formatter = new QFormatter(formattingContext);
        final CodeStyleSettings settings = formattingContext.getCodeStyleSettings();

        final Block rootBlock = new CodeBlock(element.getNode(), formatter);
        if (log.isTraceEnabled()) {
            log.trace(FormattingModelDumper.dumpFormattingModelToString(rootBlock));
        }
        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), rootBlock, settings);
    }
}
