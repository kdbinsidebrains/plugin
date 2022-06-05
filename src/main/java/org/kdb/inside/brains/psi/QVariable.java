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

    default String getSimpleName() {
        final String name = getName();
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(i + 1);
    }

    ElementContext getVariableContext();
}
