package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;

public abstract class QReferenceBase<T extends PsiElement> extends PsiReferenceBase<T> {
    public QReferenceBase(@NotNull T element) {
        super(element, element.getTextRangeInParent());
    }

    public QReferenceBase(@NotNull T element, TextRange rangeInElement) {
        super(element, rangeInElement);
    }
}
