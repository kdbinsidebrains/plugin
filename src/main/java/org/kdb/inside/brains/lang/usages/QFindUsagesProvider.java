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
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QTypes;
import org.kdb.inside.brains.psi.QVariable;

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
        return "getType: " + element.getClass().getSimpleName();
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        return "getDescriptiveName: " + element.getClass().getSimpleName();
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (!(element instanceof QVariable)) {
            return "";
        }
        return "getNodeText: " + element.getClass().getSimpleName();
//        final QVariable var = (QVariable) element;
//        return useFullName ? var.getQualifiedName() : var.getName();
    }
}
