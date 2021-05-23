package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface QVariable extends QPsiElement {
    Set<String> IMPLICIT_VARS = Set.of("x", "y", "z");

    @NotNull
    String getName();

    /**
     * Returns full name including namespace, if any.
     *
     * @return the full variable name including namespace
     */
    @NotNull
    String getQualifiedName();


    ElementContext getVariableContext();
}
