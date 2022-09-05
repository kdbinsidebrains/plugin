package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QSymbol;

public class QSymbolReferenceProvider extends QBaseReferenceProvider<QSymbol> {
    protected QSymbolReferenceProvider() {
        super(QSymbol.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QSymbol element, @NotNull ProcessingContext context) {
        return new PsiReference[]{new QSymbolReference(element)};
    }

    public static class QSymbolReference extends QBaseReference<QSymbol> {
        public QSymbolReference(@NotNull QSymbol element) {
            super(element, new TextRange(1, element.getTextLength()));
        }

        @Override
        protected String getQualifiedName(QSymbol element) {
            return element.getText().substring(1);
        }
    }
}