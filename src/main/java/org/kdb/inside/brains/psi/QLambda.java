package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.Nullable;

public interface QLambda extends QPsiElement {
    @Nullable
    QParameters getParameters();

    String getVariables();

    String getParametersInfo();
}
