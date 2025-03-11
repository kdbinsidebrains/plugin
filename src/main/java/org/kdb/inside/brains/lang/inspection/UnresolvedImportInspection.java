package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;
import org.kdb.inside.brains.psi.QImportFunction;
import org.kdb.inside.brains.psi.QLiteralExpr;
import org.kdb.inside.brains.psi.QPsiUtil;

public class UnresolvedImportInspection extends ElementInspection<QImport> {
    public UnresolvedImportInspection() {
        super(QImport.class);
    }

    @Override
    protected void validate(@NotNull QImport element, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (element instanceof QImportFunction && !(((QImportFunction) element).getExpression() instanceof QLiteralExpr)) {
            return;
        }

        for (PsiReference reference : element.getReferences()) {
            if (!reference.isSoft() && !QPsiUtil.isResolvableReference(reference)) {
                holder.registerProblem(reference);
            }
        }
    }
}
