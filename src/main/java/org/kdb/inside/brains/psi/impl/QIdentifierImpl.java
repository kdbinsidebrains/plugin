package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QIdentifier;

public abstract class QIdentifierImpl extends QPsiElementImpl implements QIdentifier {
    public QIdentifierImpl(ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return this;
    }
}
