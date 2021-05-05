package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QReturn;

public class QReturnAnnotator extends QElementAnnotator<QReturn> {
    public QReturnAnnotator() {
        super(QReturn.class);
    }

    @Override
    public void annotate(@NotNull QReturn element, @NotNull AnnotationHolder holder) {
        if (PsiTreeUtil.getParentOfType(element, QLambda.class) == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Return statement outside of a lambda").range(element).create();
        }
    }
}
