package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImportElement;

public class QImportReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof QImportElement)) {
            return PsiReference.EMPTY_ARRAY;
        }

        final QImportElement imp = (QImportElement) element;
        final TextRange range = imp.getFilepathRange();
        if (range == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        final int startOffset = range.getStartOffset();
        final String text = range.substring(element.getText()).trim();
        return new FileReferenceSet(text, element, startOffset, null, true, false) {
            @Override
            public boolean isAbsolutePathReference() {
                return true;
            }
        }.getAllReferences();
    }
}
