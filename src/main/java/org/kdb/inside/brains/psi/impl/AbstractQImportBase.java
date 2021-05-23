package org.kdb.inside.brains.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import icons.KdbIcons;
import org.kdb.inside.brains.psi.QImport;

import javax.swing.*;

public abstract class AbstractQImportBase extends QPsiElementImpl implements QImport {
    public AbstractQImportBase(ASTNode node) {
        super(node);
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            public Icon getIcon(boolean unused) {
                return KdbIcons.Node.Load;
            }

            @Override
            public String getPresentableText() {
                return getFilepath();
            }

            @Override
            public String getLocationString() {
                return null;
            }
        };
    }
}
