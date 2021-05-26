package org.kdb.inside.brains.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.NavigatablePsiElement;

public interface QImport extends QPsiElement, NavigatablePsiElement {
    TextRange getFilepathRange();

    default String getFilepath() {
        return getFilepathRange().substring(getText());
    }
}
