package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QCommand;
import org.kdb.inside.brains.psi.QPsiElementImpl;

import javax.swing.*;

public abstract class QCommandMixIn extends QPsiElementImpl implements QCommand {
    public QCommandMixIn(ASTNode node) {
        super(node);
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Command;
    }
}