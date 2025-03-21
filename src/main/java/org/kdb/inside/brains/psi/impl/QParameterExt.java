package org.kdb.inside.brains.psi.impl;

import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QPsiElement;

public interface QParameterExt extends QPsiElement {
    @NotNull
    String getParameterInfo();
}
