package org.kdb.inside.brains.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.NavigatablePsiElement;

public interface QImportElement extends QPsiElement, NavigatablePsiElement {
    TextRange getFilepathRange();
}
