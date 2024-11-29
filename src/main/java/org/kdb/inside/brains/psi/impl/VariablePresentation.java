package org.kdb.inside.brains.psi.impl;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiQualifiedNamedElement;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class VariablePresentation implements ItemPresentation {
    private final Icon icon;
    private final PsiQualifiedNamedElement element;

    public VariablePresentation(PsiQualifiedNamedElement element, Icon icon) {
        this.element = element;
        this.icon = icon;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return icon;
    }

    @Override
    public @Nullable String getPresentableText() {
        return element.getQualifiedName();
    }

    @Override
    public @Nullable String getLocationString() {
        final PsiFile containingFile = element.getContainingFile();
        return containingFile == null ? "" : containingFile.getName();
    }
}
