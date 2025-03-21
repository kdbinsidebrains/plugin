package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

public abstract class QTypedParameterMixin extends QPsiElementImpl implements QTypedParameter {
    public QTypedParameterMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull String getParameterInfo() {
        final String name = getVarDeclaration().getName();
        final QExpression expression = getExpression();
        if (expression instanceof QLiteralExpr l) {
            final QSymbol symbol = l.getSymbol();
            if (symbol != null) {
                return name + ":" + symbol.getText();
            }
        } else if (expression instanceof QLambdaExpr || expression instanceof QVarReference) {
            return name + ":λ";
        }
        return name;
    }
}
