package org.kdb.inside.brains.lang.annotation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QPsiElement;

public abstract class QElementAnnotator<T extends QPsiElement> {
    private final Class<T> type;

    public QElementAnnotator(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        annotate((T) element, holder);
    }

    protected abstract void annotate(@NotNull T element, @NotNull AnnotationHolder holder);
}
