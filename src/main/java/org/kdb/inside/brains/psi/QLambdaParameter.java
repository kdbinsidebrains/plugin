package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;

public interface QLambdaParameter extends QPsiElement {
    @NotNull
    String getParameterInfo();
}
