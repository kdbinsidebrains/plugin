package org.kdb.inside.brains.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;

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
    public PsiReference @NotNull [] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this);
    }
}