package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.psi.ElementScope;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.QVariable;

public class QRainbowVisitor extends RainbowVisitor {
    @Override
    public boolean suitableForFile(@NotNull PsiFile psiFile) {
        return QFileType.is(psiFile);
    }

    @Override
    public void visit(@NotNull PsiElement psiElement) {
        if (psiElement instanceof @NotNull QVariable var && !var.isGlobal()) {
            addInfo(createVariableInfo(var));
        }
    }

    private HighlightInfo createVariableInfo(@NotNull QVariable var) {
        final QLambda lambda = var.getContext(QLambda.class);
        if (lambda == null) {
            return null;
        }

        if (var instanceof QVarDeclaration d) {
            return getInfo(var, d, lambda);
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

    private HighlightInfo getInfo(QVariable v, QVarDeclaration d, QLambda lambda) {
        if (d.isGlobal()) {
            return null;
        }

        final ElementScope scope = d.getVariableContext().getScope();
        // only root variables, no tables, dicts and so on
        if (scope == ElementScope.PARAMETERS || scope == ElementScope.LAMBDA) {
            return getInfo(lambda, v, v.getName(), QSyntaxHighlighter.VARIABLE);
        }
        return null;
    }

    @Override
    public @NotNull HighlightVisitor clone() {
        return new QRainbowVisitor();
    }
}