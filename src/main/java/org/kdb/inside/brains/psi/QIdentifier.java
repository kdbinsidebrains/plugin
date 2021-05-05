package org.kdb.inside.brains.psi;

import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * Base entity interface and supports renaming and is navigatable.
 */
public interface QIdentifier extends QPsiElement, PsiNameIdentifierOwner, NavigatablePsiElement {
}
