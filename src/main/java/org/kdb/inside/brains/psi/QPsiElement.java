package org.kdb.inside.brains.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;

public interface QPsiElement extends PsiElement {
    default <T extends PsiElement> T getContext(Class<T> type) {
        return PsiTreeUtil.getContextOfType(this, type);
    }
}
