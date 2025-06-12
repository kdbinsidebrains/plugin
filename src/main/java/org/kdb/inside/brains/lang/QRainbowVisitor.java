package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.psi.*;

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
        final String name = var.getName();
        final ElementContext context = ElementContext.of(var);
        final ElementScope scope = context.getScope();

        if (scope == ElementScope.PARAMETERS) {
            return getInfo(context.parameters(), var, name, QSyntaxHighlighter.VARIABLE);
        } else if (scope == ElementScope.LAMBDA) {
            final QLambdaExpr lambda = context.lambda();
            if (QPsiUtil.isInnerDeclaration(lambda, name)) {
                return getInfo(context.getElement(), var, var.getName(), QSyntaxHighlighter.VARIABLE);
            }
        }
        return null;
    }

    @Override
    public @NotNull HighlightVisitor clone() {
        return new QRainbowVisitor();
    }
}
