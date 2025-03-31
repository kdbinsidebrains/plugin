package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QImport;
import org.kdb.inside.brains.psi.QPsiElementImpl;

import javax.swing.*;

public abstract class QImportCommandMixin extends QPsiElementImpl implements QImport {
    public QImportCommandMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Import;
    }
}
