package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VarAssignment(@NotNull QAssignmentExpr assignment,
                            @NotNull QVarDeclaration declaration,
                            @Nullable QExpression expression) {
}