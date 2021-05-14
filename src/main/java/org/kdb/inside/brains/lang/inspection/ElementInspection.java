package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QPsiElement;

public abstract class ElementInspection<T extends QPsiElement> extends LocalInspectionTool {
    protected final Class<T> type;

    public ElementInspection(Class<T> type) {
        this.type = type;
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (type.isAssignableFrom(element.getClass())) {
                    validate(type.cast(element), holder, isOnTheFly);
                }
            }
        };
    }

    protected abstract void validate(@NotNull T element, @NotNull ProblemsHolder holder, boolean isOnTheFly);
}
