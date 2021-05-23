package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.QVariable;

import javax.swing.*;
import java.util.Optional;

public abstract class QVariableDeclarationImpl extends QVariableBase implements QVarDeclaration {
    public QVariableDeclarationImpl(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public PsiElement setName(@NotNull String newName) {
        Optional.ofNullable(QPsiUtil.createVarDeclaration(getProject(), newName))
                .map(QVariable::getFirstChild)
                .map(PsiElement::getNode)
                .ifPresent(newKeyNode -> {
                    final ASTNode keyNode = getNode().getFirstChildNode();
                    getNode().replaceChild(keyNode, newKeyNode);
                });
        return this;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @NotNull
            @Override
            public String getPresentableText() {
                return getQualifiedName();
            }

            @NotNull
            @Override
            public String getLocationString() {
                final PsiFile containingFile = getContainingFile();
                return containingFile == null ? "" : containingFile.getName();
            }

            @NotNull
            @Override
            public Icon getIcon(boolean unused) {
                // TODO: Not implemented yet. Icon should depends on the variable type and visibility
                return KdbIcons.Node.variablePublic;
            }
        };
    }
}
