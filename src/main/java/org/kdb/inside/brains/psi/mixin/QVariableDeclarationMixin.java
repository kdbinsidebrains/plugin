package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Optional;

public abstract class QVariableDeclarationMixin extends QVariableBase implements QVarDeclaration {
    public QVariableDeclarationMixin(ASTNode node) {
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
}
