package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.psi.*;

public class QRainbowVisitor extends RainbowVisitor {
    @Override
    public boolean suitableForFile(@NotNull PsiFile psiFile) {
        return QFileType.is(psiFile);
    }

    @Override
    public void visit(@NotNull PsiElement psiElement) {
        if (psiElement instanceof @NotNull QVariable var) {
            final HighlightInfo info = createVariableInfo(var);
            if (info != null) {
                addInfo(info);
            }
        }
    }

    private @Nullable HighlightInfo createVariableInfo(@NotNull QVariable var) {
        final QLambda lambda = var.getContext(QLambda.class);
        if (lambda == null) {
            return null;
        }

        if (var instanceof QVarDeclaration d) {
            return getInfo(var, d, lambda);
        }

        if (lambda.isImplicitDeclaration(var)) {
            return createInfo(lambda, var);
        }

        final PsiReference[] references = var.getReferences();
        for (PsiReference reference : references) {
            final PsiElement resolve = reference.resolve();
            if (resolve instanceof QVarDeclaration d) {
                final HighlightInfo info = getInfo(var, d, lambda);
                if (info != null) {
                    return info;
                }
            }
        }
        return null;
    }

    private @Nullable HighlightInfo getInfo(@NotNull QVariable var, @NotNull QVarDeclaration dec, @NotNull QLambda lambda) {
        if (QPsiUtil.isGlobalDeclaration(dec)) {
            return null;
        }

        final ElementScope scope = dec.getVariableContext().getScope();
        // only root variables, no tables, dicts and so on
        if (scope == ElementScope.PARAMETERS || scope == ElementScope.LAMBDA) {
            return createInfo(lambda, var);
        }
        return null;
    }

    @Override
    public @NotNull HighlightVisitor clone() {
        return new QRainbowVisitor();
    }

    private @NotNull HighlightInfo createInfo(@NotNull QLambda lambda, @NotNull QVariable var) {
        return getInfo(lambda, var, var.getName(), QSyntaxHighlighter.VARIABLE);
    }
}