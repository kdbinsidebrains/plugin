package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.impl.QExpressionImpl;

import java.util.ArrayList;
import java.util.List;

import static org.kdb.inside.brains.psi.QTypes.SEMICOLON;

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
        return List.of();
    }

    private @NotNull List<VarAssignment> getVarDeclarationItems(QVarDeclaration vd) {
        return List.of(new VarAssignment(this, vd, getExpression()));
    }

    private @NotNull List<VarAssignment> getPatternDeclarationItems(QPatternDeclaration pd) {
        final List<QTypedVariable> vars = getTypedVariables(pd);
        final List<QExpression> expressions = getExpression() instanceof QParenthesesExpr qp ? qp.getExpressionList() : List.of();

        final int varsSize = vars.size();
        final int expressionsSize = expressions.size();

        final List<VarAssignment> res = new ArrayList<>();
        for (int i = 0, j = 0; i < varsSize; i++) {
            final QTypedVariable var = vars.get(i);
            if (var != null) { // empty variables - we ignore but could as an expression
                if (isProjection(var)) {
                    continue;
                }
                res.add(new VarAssignment(this, var.getVarDeclaration(), j < expressionsSize ? expressions.get(j++) : null));
            } else {
                j++;
            }
        }
        return res;
    }

    private boolean isProjection(QTypedVariable var) {
        return var.getAssignmentType() != null && var.getExpression() == null;
    }

    private List<QTypedVariable> getTypedVariables(QPatternDeclaration pd) {
        PsiElement re = pd.getFirstChild();
        final List<QTypedVariable> res = new ArrayList<>();
        QTypedVariable curr = null;
        while (re != null) {
            if (re instanceof QTypedVariable tv) {
                curr = tv;
            } else {
                final IElementType tokenType = re.getNode().getElementType();
                if (tokenType == SEMICOLON) {
                    res.add(curr);
                    curr = null;
                }
            }
            re = re.getNextSibling();
        }
        if (curr != null) {
            res.add(curr);
        }
        return res;
    }
}