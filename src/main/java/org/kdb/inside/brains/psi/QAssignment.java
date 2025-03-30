package org.kdb.inside.brains.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface QAssignment extends QExpression {
    @NotNull List<VarAssignment> getVarAssignments();


//    default boolean isGlobalAssignment() {
//        final QAssignmentType assignmentType = getAssignmentType();
//        return assignmentType != null && "::".equals(assignmentType.getText());
//    }
}
