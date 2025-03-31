package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.impl.QExpressionImpl;

import java.util.ArrayList;
import java.util.List;

public abstract class QAssignmentMixin extends QExpressionImpl implements QAssignmentExpr {
    public QAssignmentMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull List<VarAssignment> getVarAssignments() {
        final QVarDeclaration vd = getVarDeclaration();
        if (vd != null) {
            return getVarDeclarationItems(vd);
        }

        final QPatternDeclaration pd = getPatternDeclaration();
        if (pd != null) {
            return getPatternDeclarationItems(pd);
        }
        final QVarIndexing vi = getVarIndexing();
        if (vi != null) {
            return getVarIndexingItems(vi);
        }
        return List.of();
    }

    private @NotNull List<VarAssignment> getVarIndexingItems(QVarIndexing vi) {
        // TODO: not implemented
        return List.of();
    }

    private @NotNull List<VarAssignment> getVarDeclarationItems(QVarDeclaration vd) {
        return List.of(new VarAssignment(this, vd, getExpression()));
    }

    private @NotNull List<VarAssignment> getPatternDeclarationItems(QPatternDeclaration pd) {
        final List<QTypedVariable> vars = pd.getOrderedTypedVariables();
        final List<QExpression> expressions = getExpression() instanceof QParenthesesExpr qp ? qp.getExpressionList() : List.of();

        final int varsSize = vars.size();
        final int expressionsSize = expressions.size();

        final List<VarAssignment> res = new ArrayList<>();
        for (int i = 0; i < varsSize; i++) {
            final QTypedVariable var = vars.get(i);
            if (var != null) { // we ignore empty variables
                res.add(new VarAssignment(this, var.getVarDeclaration(), i < expressionsSize ? expressions.get(i) : null));
            }
        }
        return res;
    }
}