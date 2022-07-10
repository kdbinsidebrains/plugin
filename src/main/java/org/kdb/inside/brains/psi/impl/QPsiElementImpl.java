package org.kdb.inside.brains.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class QPsiElementImpl extends ASTWrapperPsiElement {
    public QPsiElementImpl(ASTNode node) {
        super(node);
    }

    @Override
    public PsiReference getReference() {
        final PsiReference[] references = getReferences();
        if (references.length == 1) {
            return references[0];
        }
        return null;
    }

    @Override
    protected @Nullable Icon getElementIcon(int flags) {
        return super.getElementIcon(flags);
    }

    @Override
    public PsiReference @NotNull [] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }
}