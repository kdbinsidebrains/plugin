package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;

import java.util.Collection;

import static java.util.Collections.emptyList;

public class QImportReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof QImport)) {
            return PsiReference.EMPTY_ARRAY;
        }

        final QImport imp = (QImport) element;
        final TextRange range = imp.getFilepathRange();
        if (range == null) {
            return PsiReference.EMPTY_ARRAY;
        }

        final int startOffset = range.getStartOffset();
        final String text = range.substring(element.getText()).trim();

        return new FileReferenceSet(text, element, startOffset, null, true, false) {
            @Override
            public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
                final PsiFile file = getContainingFile();
                if (file == null) {
                    return emptyList();
                }
                return getAbsoluteTopLevelDirLocations(file);
            }
        }.getAllReferences();
    }
}
