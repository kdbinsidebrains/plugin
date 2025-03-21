package org.kdb.inside.brains.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QParameters;
import org.kdb.inside.brains.psi.QPsiElement;

public interface QLambdaExt extends QPsiElement {
    @Nullable
    QParameters getParameters();

    default String getVariables() {
        final QParameters parameters = getParameters();
        return parameters == null ? "[]" : parameters.getParametersInfo();
    }

    @NotNull
    default String getParametersInfo() {
        final QParameters parameters = getParameters();
        if (parameters == null) {
            return "[]";
        }
        return parameters.getParametersInfo();
    }
}
