package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface QAssignment extends QExpression {
    @NotNull List<VarAssignment> getVarAssignments();
}
