package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.*;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
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
        final String name = variable.getName();
        final QParameters parameters = getParameters();
        if (parameters != null) {
            final QVarDeclaration d = findInParameters(parameters, name);
            if (d != null) {
                return d;
            }
        } else if (QPsiUtil.isImplicitName(name)) {
            return null;
        }

        final QExpressions expressions = getExpressions();
        if (expressions != null) {
            return findInExpressions(expressions, variable, name);
        }
        return null;
    }

    private QVarDeclaration findInParameters(QParameters parameters, String name) {
        final DeclarationsVisitor visitor = new DeclarationsVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof @NotNull QVarDeclaration d) {
                    if (d.getName().equals(name)) {
                        declaration = d;
                        stopWalking();
                    }
                } else {
                    super.visitElement(element);
                }
            }
        };
        parameters.acceptChildren(visitor);
        return visitor.declaration;
    }

    private QVarDeclaration findInExpressions(QExpressions expressions, QVariable variable, String name) {
        final DeclarationsVisitor visitor = new DeclarationsVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                // we don't search inside others lambda, query, table or dict
                if (isContextualElement(element)) {
                    return;
                }

                // no reason to search after the definition.// In a lambda it can't be below.
                if (element.equals(variable)) {
                    containsVariable = true;
                } else if (element instanceof QVarDeclaration d) {
                    if (name.equals(d.getName()) && !QPsiUtil.isGlobalDeclaration(d)) {
                        declaration = d;
                    }
                } else {
                    super.visitElement(element);
                }
            }
        };

        for (QExpression expression : expressions.getExpressionList()) {
            // We iterate over each expression and takes the right as the best because Q is right-to-left language
            expression.accept(visitor);

            // if the declaration found - grate;
            // if the expression contains the original variable and no declaration in the expression, we stop anyway.
            if (visitor.declaration != null || visitor.containsVariable) {
                return visitor.declaration;
            }
        }
        return null;
    }

    private boolean isContextualElement(@NotNull PsiElement element) {
        return element instanceof QLambdaExpr || element instanceof QQueryExpr || element instanceof QTableExpr || element instanceof QDictExpr;
    }

    private static class DeclarationsVisitor extends PsiRecursiveElementWalkingVisitor {
        protected boolean containsVariable;
        protected QVarDeclaration declaration;

        @Override
        public void visitComment(@NotNull PsiComment ignore) {
        }

        @Override
        public void visitPlainText(@NotNull PsiPlainText ignore) {
        }

        @Override
        public void visitWhiteSpace(@NotNull PsiWhiteSpace ignore) {
        }

        @Override
        public void visitErrorElement(@NotNull PsiErrorElement ignore) {
        }

        @Override
        public void visitOuterLanguageElement(@NotNull OuterLanguageElement ignore) {
        }
    }
}