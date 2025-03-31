package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QParameters;
import org.kdb.inside.brains.psi.impl.QExpressionImpl;

import javax.swing.*;

public abstract class QLambdaMixin extends QExpressionImpl implements QLambda {
    public QLambdaMixin(ASTNode node) {
        super(node);
    }

    @Override
    public String getVariables() {
        final QParameters parameters = getParameters();
        return parameters == null ? "[]" : parameters.getParametersInfo();
    }

    @Override
    public @Nullable Icon getIcon(int flags) {
        return KdbIcons.Node.Lambda;
    }

    @Override
    public String getParametersInfo() {
        final QParameters parameters = getParameters();
        if (parameters == null) {
            return "[]";
        }
        return parameters.getParametersInfo();
    }
}