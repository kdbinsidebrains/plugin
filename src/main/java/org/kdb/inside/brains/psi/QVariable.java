package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;

public interface QVariable extends QPsiElement {
    /**
     * Returns the name as defined in the code without possible context.
     */
    @NotNull
    String getName();

    /**
     * Returns full name including namespace, if any.
     *
     * @return the full variable name including namespace
     */
    @NotNull
    String getQualifiedName();

    /**
     * Returns last tone of the name or the full name.
     */
    default String getSimpleName() {
        final String name = getName();
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(i + 1);
    }

    ElementContext getVariableContext();
}
