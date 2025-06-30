package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.impl.QExpressionImpl;

import javax.swing.*;

public abstract class QLambdaMixin extends QExpressionImpl implements QLambda {
    public QLambdaMixin(ASTNode node) {
        super(node);
    }

    @Override
    public String getVariables() {
        final QParameters parameters = getParameters();
        return parameters == null ? "[]" : parameters.getParametersInfo();
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Lambda;
    }

    @Override
    public String getParametersInfo() {
        final QParameters parameters = getParameters();
        if (parameters == null) {
            return "[]";
        }
        return parameters.getParametersInfo();
    }

    @Override
    public boolean isImplicitDeclaration(@NotNull QVariable variable) {
        return getParameters() == null && QPsiUtil.isImplicitName(variable.getName());
    }

    @Override
    public @Nullable QVarDeclaration getLocalDeclaration(@NotNull QVariable variable) {
        // implicit variable or in parameters list - ignore namespace
        if (isImplicitDeclaration(variable)) {
            return null;
        }

        final String name = variable.getName();
        final QVarDeclaration[] res = new QVarDeclaration[1];
        acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                // we don't search inside others lambda, query, table or dict
                if (QPsiUtil.isLocalContextElement(element)) {
                    return;
                }

                // no reason to search after the definition.// In a lambda it can't be below.
                if (element.equals(variable)) {
                    stopWalking();
                } else if (element instanceof QVarDeclaration d) {
                    if (name.equals(d.getName()) && !QPsiUtil.isGlobalDeclaration(d)) {
                        res[0] = d;
                        stopWalking();
                    }
                } else {
                    super.visitElement(element);
                }
            }
        });
        return res[0];
    }
}