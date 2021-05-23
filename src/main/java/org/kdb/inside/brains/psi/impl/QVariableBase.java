package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.List;

public class QVariableBase extends QPsiElementImpl implements QVariable {
    private String name = null;
    private String qualifiedName = null;

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
        if (qualifiedName == null) {
            qualifiedName = calculateQualifiedName();
        }
        return qualifiedName;
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
        final QLambda lambda = getContext(QLambda.class);
        if (lambda != null) {
            final QParameters lambdaParams = lambda.getParameters();
            // implicit variable or in parameters list - ignore namespace
            if (lambdaParams == null) {
                if (QVariable.IMPLICIT_VARS.contains(name)) {
                    return name;
                }
            } else {
                final List<QVarDeclaration> variables = lambdaParams.getVariables();
                if (variables.contains(this)) {
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
        qualifiedName = null;
    }
}
