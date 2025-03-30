package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QContext;
import org.kdb.inside.brains.psi.QPsiElementImpl;
import org.kdb.inside.brains.psi.QVariable;

import javax.swing.*;
import java.util.Objects;

public abstract class QContextMixin extends QPsiElementImpl implements QContext {
    private String contextName = null;

    public QContextMixin(ASTNode node) {
        super(node);
        updateContextName();
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Context;
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
