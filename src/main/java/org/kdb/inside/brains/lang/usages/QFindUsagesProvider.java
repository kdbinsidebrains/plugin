package org.kdb.inside.brains.lang.usages;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLexer;
import org.kdb.inside.brains.QParserDefinition;
import org.kdb.inside.brains.psi.*;

public final class QFindUsagesProvider implements FindUsagesProvider {
    @NotNull
    @Override
    public WordsScanner getWordsScanner() {
        final DefaultWordsScanner scanner = new DefaultWordsScanner(
                QLexer.newLexer(),
                TokenSet.create(QTypes.VAR_DECLARATION, QTypes.VAR_REFERENCE, QTypes.SYMBOL),
                QParserDefinition.COMMENTS,
                TokenSet.EMPTY);
        scanner.setMayHaveFileRefsInLiterals(true);
        return scanner;
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof QVariable || psiElement instanceof QSymbol;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof QVarReference) {
            return "variable reference";
        }
        if (element instanceof QVarDeclaration) {
            return "variable declaration";
        }
        if (element instanceof QSymbol) {
            return "symbol";
        }
        return "";
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        return getNodeText(element, true);
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (element instanceof QVariable) {
            final QVariable var = (QVariable) element;
            return useFullName ? var.getQualifiedName() : var.getName();
        }
        if (element instanceof QSymbol) {
            return element.getText();
        }
        return element.getText();
    }
}
