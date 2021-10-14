package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;

public class QVariableBase extends QPsiElementImpl implements QVariable {
    private String name = null;

    public QVariableBase(ASTNode node) {
        super(node);
    }

    @NotNull
    public String getName() {
        if (name == null) {
            name = getText();
            if (name.endsWith("IntellijIdeaRulezzz")) {
                name = name.substring(0, name.length() - 19);
            }
        }
        return name;
    }

    @NotNull
    @Override
    public String getQualifiedName() {
        return calculateQualifiedName();
    }

    @Override
    public ElementContext getVariableContext() {
        return ElementContext.of(this);
    }

    @NotNull
    private String calculateQualifiedName() {
        final String name = getName();

        // if has namespace - it's global in any case
        if (QPsiUtil.hasNamespace(name)) {
            return name;
        }

        // It's namespace name itself - ignore
        if (getParent() instanceof QContext) {
            return name;
        }

        // No namespace - ignore
        final QContext context = PsiTreeUtil.getParentOfType(this, QContext.class);
        if (context == null || context.getVariable() == null) {
            return name;
        }

        // root namespace - ignore
        final String namespaceName = context.getVariable().getName();
        if (".".equals(namespaceName)) {
            return name;
        }

        // no lambda - return full name
        final QLambdaExpr lambda = getContext(QLambdaExpr.class);
        if (lambda != null) {
            // implicit variable or in parameters list - ignore namespace
            if (lambda.getParameters() == null && QVariable.IMPLICIT_VARS.contains(name)) {
                return name;
            }

            final Collection<QVarDeclaration> declarations = PsiTreeUtil.findChildrenOfType(lambda, QVarDeclaration.class);
            for (QVarDeclaration declaration : declarations) {
                // Same name and same lambda - internal variable
                if (name.equals(declaration.getName()) && !QPsiUtil.isGlobalDeclaration(declaration)) {
                    return name;
                }
            }
        }

        return QPsiUtil.createQualifiedName(namespaceName, name);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        invalidate();
    }

    void invalidate() {
        name = null;
    }
}
