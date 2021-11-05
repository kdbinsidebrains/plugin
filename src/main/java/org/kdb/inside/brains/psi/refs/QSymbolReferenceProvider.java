package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QSymbol;

public class QSymbolReferenceProvider extends QReferenceProvider<QSymbol> {
    protected QSymbolReferenceProvider() {
        super(QSymbol.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QSymbol element, @NotNull ProcessingContext context) {
        return PsiReference.EMPTY_ARRAY;
    }
}
