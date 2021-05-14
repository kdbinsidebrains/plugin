package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImportElement;

public class UnresolvedImportInspection extends ElementInspection<QImportElement> {
    public UnresolvedImportInspection() {
        super(QImportElement.class);
    }

    @Override
    protected void validate(@NotNull QImportElement element, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        checkReferences(element.getReferences(), holder);
    }

    private void checkReferences(PsiReference[] references, ProblemsHolder holder) {
        for (PsiReference reference : references) {
            if (reference.isSoft()) {
                continue;
            }

            if (reference.resolve() != null) {
                continue;
            }

            if (reference instanceof PsiPolyVariantReference) {
                final PsiPolyVariantReference pvr = (PsiPolyVariantReference) reference;
                if (pvr.multiResolve(false).length != 0) {
                    continue;
                }
            }

            holder.registerProblem(reference);
        }
    }
}
