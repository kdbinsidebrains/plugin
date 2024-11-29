package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QSymbol;

import java.util.Optional;

public class QSymbolElementImpl extends QPsiElementImpl implements QSymbol {
    public QSymbolElementImpl(ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        return getText().substring(1);
    }

    @Override
    public @Nullable String getQualifiedName() {
        return getName();
    }

    @Override
    public ItemPresentation getPresentation() {
        return new VariablePresentation(this, KdbIcons.Node.Symbol);
    }

    @Override
    public int getTextOffset() {
        return super.getTextOffset() + 1;
    }

    @Override
    public @Nullable PsiElement getNameIdentifier() {
        return this;
    }

    @Override
    public PsiElement setName(@NotNull String newName) throws IncorrectOperationException {
        Optional.ofNullable(QPsiUtil.createSymbol(getProject(), newName.charAt(0) == '`' ? newName : "`" + newName))
                .map(QSymbol::getFirstChild)
                .map(PsiElement::getNode)
                .ifPresent(newKeyNode -> {
                    final ASTNode keyNode = getNode().getFirstChildNode();
                    getNode().replaceChild(keyNode, newKeyNode);
                });
        return this;
    }
}