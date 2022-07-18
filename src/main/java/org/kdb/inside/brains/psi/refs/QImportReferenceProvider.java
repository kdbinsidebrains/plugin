package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;

import java.util.Collection;

import static java.util.Collections.emptyList;

public class QImportReferenceProvider extends QBaseReferenceProvider<QImport> {
    protected QImportReferenceProvider() {
        super(QImport.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QImport element, @NotNull ProcessingContext context) {
        final TextRange range = element.getFilePathRange();
        if (range.isEmpty()) {
            return PsiReference.EMPTY_ARRAY;
        }

        return new FileReferenceSet(element.getFilePath(), element, range.getStartOffset(), null, true, false) {
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
