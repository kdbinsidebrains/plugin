package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.kdb.inside.brains.psi.QPsiElement;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Objects;

public class QPsiContextImpl extends QPsiElementImpl implements QPsiElement {
    private String contextName = null;

    public QPsiContextImpl(ASTNode node) {
        super(node);
        updateContextName();
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();

        if (updateContextName()) {
            invalidateVariables();
        }
    }

    private boolean updateContextName() {
        final QVariable var = findChildByClass(QVariable.class);
        final String name = var == null ? null : var.getText();
        if (Objects.equals(contextName, name)) {
            return false;
        }
        contextName = name;
        return true;
    }

    private void invalidateVariables() {
        PsiTreeUtil.findChildrenOfType(this, QVariableBase.class).forEach(QVariableBase::invalidate);
    }
}
