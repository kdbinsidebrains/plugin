package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QQueryExpr;
import org.kdb.inside.brains.psi.QTableExpr;
import org.kdb.inside.brains.psi.QVarReference;

public class UndefinedVariableInspection extends ElementInspection<QVarReference> {
    public UndefinedVariableInspection() {
        super(QVarReference.class);
    }

    @Override
    protected void validate(@NotNull QVarReference variable, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final String variableName = variable.getQualifiedName();
        if (QLanguage.isKeyword(variableName) || QLanguage.isSystemFunction(variableName)) {
            return;
        }

        if (QPsiUtil.isImplicitVariable(variable)) {
            return;
        }

        if (variable.getContext(QQueryExpr.class) != null) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        if (variable.getContext(QTableExpr.class) != null) {
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
