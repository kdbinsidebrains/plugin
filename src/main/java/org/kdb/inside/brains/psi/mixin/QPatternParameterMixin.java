package org.kdb.inside.brains.psi.mixin;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QParameter;
import org.kdb.inside.brains.psi.QPatternParameter;
import org.kdb.inside.brains.psi.QPsiElementImpl;

import java.util.stream.Collectors;

public abstract class QPatternParameterMixin extends QPsiElementImpl implements QPatternParameter {
    public QPatternParameterMixin(ASTNode node) {
        super(node);
    }

    @Override
    public @NotNull String getParameterInfo() {
        return "(" + getParameters().stream().map(QParameter::getParameterInfo).collect(Collectors.joining(";")) + ")";
    }
}
