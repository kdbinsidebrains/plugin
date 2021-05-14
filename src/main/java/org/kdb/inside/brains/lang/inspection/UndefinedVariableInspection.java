package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QQuery;
import org.kdb.inside.brains.psi.QTable;
import org.kdb.inside.brains.psi.QVariable;

public class UndefinedVariableInspection extends ElementInspection<QVariable> {
    public UndefinedVariableInspection() {
        super(QVariable.class);
    }

    @Override
    protected void validate(@NotNull QVariable variable, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (QPsiUtil.getAssignmentType(variable) == null) {
            validateDeclaration(variable, holder);
        }
    }

    private void validateDeclaration(QVariable variable, ProblemsHolder holder) {
        final String variableName = variable.getQualifiedName();
        if (QLanguage.isKeyword(variableName) || QLanguage.isSystemFunction(variableName)) {
            return;
        }

        if (QPsiUtil.isImplicitVariable(variable)) {
            return;
        }

        if (variable.getContext(QQuery.class) != null) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        if (variable.getContext(QTable.class) != null) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        final PsiReference reference = variable.getReference();
        if (reference instanceof PsiPolyVariantReference) {
            final ResolveResult[] resolveResults = ((PsiPolyVariantReference) reference).multiResolve(false);
            if (resolveResults.length != 0) {
                return;
            }
        }
        holder.registerProblem(variable, "`" + variableName + "` might not have been defined", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL);
    }
}
