package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImportElement;

import javax.swing.*;

public class QImportElementImpl extends QPsiElementImpl implements QImportElement {
    public QImportElementImpl(ASTNode node) {
        super(node);
    }

    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @NotNull
            @Override
            public String getPresentableText() {
                final String text = getText();
                return text.substring(text.indexOf(' ') + 1).trim();
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
                return KdbIcons.Main.Import;
            }
        };
    }
}