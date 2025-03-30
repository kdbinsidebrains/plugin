package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QImportFunction;
import org.kdb.inside.brains.psi.impl.QExpressionImpl;

import javax.swing.*;

public abstract class QImportFunctionMixin extends QExpressionImpl implements QImportFunction {
    public QImportFunctionMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Import;
    }
}
