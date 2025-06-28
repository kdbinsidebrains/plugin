package org.kdb.inside.brains.lang;

import com.intellij.codeInsight.daemon.RainbowVisitor;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
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
        if (var.isGlobal()) {
            return null;
        }

        final QLambda lambda = PsiTreeUtil.getParentOfType(var, QLambda.class);
        if (lambda == null) {
            return null;
        }

        final String name = var.getName();
        final ElementContext context = ElementContext.of(var);
        if (context.is(ElementScope.PARAMETERS)) {
            return getInfo(context.parameters(), var, name, QSyntaxHighlighter.VARIABLE);
        }

        if (context.is(ElementScope.LAMBDA) && QPsiUtil.getLocalDefinition(context.lambda(), var) != null) {
            return getInfo(context.lambda(), var, name, QSyntaxHighlighter.VARIABLE);
        }

        if (var instanceof QVarReference && context.any(ElementScope.DICT, ElementScope.TABLE) && QPsiUtil.getLocalDefinition(lambda, var) != null) {
            return getInfo(lambda, var, name, QSyntaxHighlighter.VARIABLE);
        }

//        final QLambdaExpr l = var.getContext(QLambdaExpr.class);
//        final QLambdaExpr lambda = findRootLambda(var);
//        if (lambda != null && QPsiUtil.isInnerDeclaration(lambda, name)) {
//            return getInfo(lambda, var, name, QSyntaxHighlighter.VARIABLE);
//        }
        return null;
    }

    private QLambdaExpr findRootLambda(@NotNull QVariable var) {
        ElementContext ctx = ElementContext.of(var);
        while (!ctx.isFile()) {
            if (ctx.is(ElementScope.LAMBDA)) {
                return ctx.lambda();
            }
            ctx = ctx.getParent();
        }
        return null;
    }

    @Override
    public @NotNull HighlightVisitor clone() {
        return new QRainbowVisitor();
    }
}