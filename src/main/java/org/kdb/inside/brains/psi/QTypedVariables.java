package org.kdb.inside.brains.psi;

import java.util.List;

public interface QTypedVariables extends QPsiElement {
    /**
     * Returns ordered the list of typed variables there some values can be null.
     */
    List<QTypedVariable> getOrderedTypedVariables();
}
