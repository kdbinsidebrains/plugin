package org.kdb.inside.brains.lang.usage;

import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QAssignment;
import org.kdb.inside.brains.psi.QParameters;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVariable;

/**
 * TODO: Symbol is not supported
 */
public final class QFindUsagesProvider implements FindUsagesProvider {
    @NotNull
    @Override
    public QWordsScanner getWordsScanner() {
        return new QWordsScanner();
    }

    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        if (!(psiElement instanceof QVariable)) {
            return false;
        }
        final PsiElement context = psiElement.getContext();
        return context instanceof QAssignment || context instanceof QParameters;
    }

    @Nullable
    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement element) {
        if (element instanceof QVariable) {
            return QPsiUtil.getFunctionDefinition((QVariable) element).isPresent() ? "function" : "variable";
        }
        return "";
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement element) {
        if (element instanceof QVariable) {
            return QPsiUtil.getDescriptiveName((QVariable) element);
        }
        return "";
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement element, boolean useFullName) {
        if (!(element instanceof QVariable)) {
            return "";
        }
        final QVariable var = (QVariable) element;
        return useFullName ? var.getQualifiedName() : var.getName();
    }
}
